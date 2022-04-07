package com.scofu.network.instance.discord;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.PeriodEscapedString;
import com.scofu.network.document.Query;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Network;
import com.scofu.network.instance.NetworkBuilder;
import com.scofu.network.instance.NetworkRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import net.renfei.cloudflare.Cloudflare;
import reactor.core.publisher.Mono;

final class Test implements Feature {

  private final GatewayDiscordClient client;
  private final NetworkRepository networkRepository;
  private final Cloudflare cloudflare;

  @Inject
  Test(GatewayDiscordClient client, NetworkRepository networkRepository) {
    this.client = client;
    this.networkRepository = networkRepository;
    this.cloudflare = new Cloudflare(System.getenv("CLOUDFLARE_TOKEN"));
    System.out.println("here1");
  }

  @Override
  public void enable() {
    System.out.println("enablus!");
    client.updatePresence(
        ClientPresence.of(Status.ONLINE, ClientActivity.watching("www.scofu.com"))).block();
    System.out.println("yo!");

    networkRepository.delete("test").join();
    networkRepository.delete("build").join();

    final var networks = networkRepository.find(Query.empty()).thenApply(Map::values).join();

    if (networks.isEmpty()) {
      networkRepository.update(Network.builder()
          .withId("test")
          .withName("Test Nätverk")
          .withDeployment("default", Deployment.builder()
              .withImage("docker.scofu.com/bukkit:latest")
              .withName("test-bukkit")
              .withNetworkId("test")
              .withGroupId("test")
              .withEnvironment("APP_WORLD_GROUP_ID", "com.scofu")
              .withEnvironment("APP_WORLD_ARTIFACT_ID", "testo")
              .build())
          .withEndpoint("test.scofu.com", "default")
          .build()).join();
    }

    networkRepository.update(Network.builder()
        .withId("build")
        .withName("Bygg Server")
        .withDeployment("default", Deployment.builder()
            .withImage("docker.scofu.com/bukkit-build:latest")
            .withName("bukkit-build")
            .withNetworkId("build")
            .withGroupId("build")
            .build())
        .withEndpoint("build.scofu.com", "default")
        .build()).join();

    final var guildId = Snowflake.of("905343563930427472");
    final var channelId = Snowflake.of("905351244204367894");

    final var createButton = Button.primary("network-create", "Skapa ett nätverk");
    final var deleteButton = Button.primary("network-delete", "Ta bort ett nätverk");
    final var confirmButton = Button.success("network-confirm", "Bekräfta");
    final var cancelButton = Button.danger("network-cancel", "Avbryt");

    client.on(GuildCreateEvent.class).subscribe(event -> {
      System.out.println("guild create!!");
      resetChannel(channelId, createButton, deleteButton).subscribe();
    });

    var states = Maps.<Snowflake, State>newConcurrentMap();

    client.on(SelectMenuInteractionEvent.class).subscribe(event -> {
      if (event.getCustomId().equals("network-select")) {
        final var id = event.getValues().size() == 1 ? event.getValues().get(0) : null;
        if (id == null) {
          return;
        }
        final var user = event.getInteraction().getUser();
        if (user.isBot()) {
          return;
        }
        var state = states.get(user.getId());
        if (state == null) {
          return;
        }
        state.networkBuilder = Network.builder().withId(id);
        state.event.deleteReply()
            .then(event.reply()
                .withEmbeds(EmbedCreateSpec.builder()
                    .title("Är du säker?")
                    .addField("ID", id, false)
                    .build())
                .withComponents(ActionRow.of(cancelButton, confirmButton)))
            .subscribe();
      }
    });

    client.on(ButtonInteractionEvent.class).subscribe(event -> {
      if (event.getInteraction().getUser().isBot()) {
        System.out.println("bot!!");
        return;
      }
      if (event.getCustomId().equals("network-create")) {
        final var userId = event.getInteraction().getUser().getId();
        states.put(userId, new State(true, event, Network.builder()));
        event.reply("Ange ID (måste vara unikt) [exempel]").subscribe();
        return;
      }
      if (event.getCustomId().equals("network-delete")) {
        final var userId = event.getInteraction().getUser().getId();
        states.put(userId, new State(false, event, Network.builder()));
        event.deferReply()
            .then(Mono.fromFuture(networkRepository.find(Query.empty()))
                .flatMapIterable(Map::values)
                .map(network -> Option.of(network.name().orElse("(inget namn)"), network.id())
                    .withDescription(network.endpoints()
                        .keySet()
                        .stream()
                        .map(PeriodEscapedString::toString)
                        .collect(Collectors.joining(", "))))
                .collectList()
                .map(options -> SelectMenu.of("network-select", options)
                    .withMinValues(1)
                    .withMaxValues(1))
                .flatMap(
                    menu -> event.editReply("Välj nätverk").withComponents(ActionRow.of(menu))))
            .subscribe();
        return;
      }
      if (event.getCustomId().equals("network-confirm")) {
        final var userId = event.getInteraction().getUser().getId();
        var state = states.get(userId);

        if (state == null) {
          return;
        }

        final var network = state.networkBuilder.build();

        if (!state.creating) {
          final var oldNetwork = networkRepository.byId(network.id()).orElse(null);
          if (oldNetwork == null) {
            state.event.editReply("Ett nätverk med ID '" + network.id() + "' finns inte!")
                .subscribe();
            return;
          }

          event.reply("Tar bort nätverk...")
              .then(Mono.fromFuture(networkRepository.delete(network.id()))
                  .doOnSuccess(unused -> states.remove(userId)))
              .then(event.deleteReply())
              .then(resetChannel(channelId, createButton, deleteButton))
              .subscribe();
          return;
        }

        final var oldNetwork = networkRepository.byId(network.id()).orElse(null);

        if (oldNetwork != null) {
          state.event.editReply("Ett nätverk med ID '" + network.id() + "' finns redan!")
              .subscribe();
          return;
        }

        state.event.deleteReply()
            .then(event.reply("Skapar nätverk..."))
            .then(Mono.fromFuture(networkRepository.update(network)).doOnSuccess(unused -> {

              states.remove(userId);

              //              try {
              //                final var zones = cloudflare.zone.getListZones();
              //                System.out.println("zones = " + zones);
              //                final var dnsRecords = cloudflare.dnsRecords.getListDnsRecord
              //                (zones.get(0).getId(),
              //                    Map.of("match", "all", "type", "SRV", "name", network.domains
              //                    ().get(0)));
              //                System.out.println("dnsRecords = " + dnsRecords);
              //                if (!dnsRecords.isEmpty()) {
              //                  return;
              //                }
              //                final var createDnsRecord = new CreateSRVDNSRecord();
              //                createDnsRecord.setType("SRV");
              //                createDnsRecord.setData(Map.of("service", "_minecraft", "proto",
              //                "_tcp", "name",
              //                    network.domains().get(0), "priority", 1, "weight", 5, "port",
              //                    25565, "target",
              //                    "mc.scofu.com"));
              //
              //                System.out.println(JSON.toJSONString(createDnsRecord));
              //
              //                cloudflare.dnsRecords.createDnsRecord(zones.get(0).getId(),
              //                createDnsRecord);
              //              } catch (IOException e) {
              //                e.printStackTrace();
              //              }

            }))
            .then(event.deleteReply())
            .then(resetChannel(channelId, createButton, deleteButton))
            .subscribe();
        return;
      }
      if (event.getCustomId().equals("network-cancel")) {
        final var userId = event.getInteraction().getUser().getId();
        var state = states.get(userId);

        if (state == null) {
          return;
        }

        states.remove(userId);
        state.event.deleteReply()
            .then(resetChannel(channelId, createButton, deleteButton))
            .subscribe();
      }
    });

    //    client.on(MessageCreateEvent.class).subscribe(event -> {
    //      if (!event.getMessage().getChannelId().equals(channelId)) {
    //        return;
    //      }
    //      final var user = event.getMessage().getAuthor().orElse(null);
    //      if (user == null || user.isBot()) {
    //        return;
    //      }
    //      var state = states.get(user.getId());
    //      if (state.networkBuilder.id() == null) {
    //        state.networkBuilder.withId(event.getMessage().getContent());
    //        if (!state.creating) {
    //          return;
    //        }
    //        event.getMessage()
    //            .delete()
    //            .then(state.event.editReply("Ange namn [Exempel Nätverk]"))
    //            .subscribe();
    //      } else if (state.networkBuilder.name() == null) {
    //        state.networkBuilder.withName(event.getMessage().getContent());
    //        event.getMessage()
    //            .delete()
    //            .then(state.event.editReply(
    //                "Ange domän(er) (måste vara unika) [exempel.com test.exempel.com]"))
    //            .subscribe();
    //
    //      } else if (state.domains == null) {
    //        state.domains = Arrays.asList(event.getMessage().getContent().split(" "));
    //        event.getMessage()
    //            .delete()
    //            .then(state.event.editReply(
    //                "Ange bild och namn för standardbild [docker.scofu.com/bukkit-empty:latest
    //                lobby]"))
    //            .subscribe();
    //
    //      } else if (state.deploymentImage == null) {
    //        final var strings = event.getMessage().getContent().split(" ");
    //        if (strings.length != 2) {
    //          event.getMessage()
    //              .delete()
    //              .then(state.event.editReply(
    //                  "Ange bild och namn för standardbild [docker.scofu
    //                  .com/bukkit-empty:latest " +
    //                      "lobby]"))
    //              .subscribe();
    //          return;
    //        }
    //        state.deploymentImage = strings[0];
    //        state.deploymentName = strings[1];
    //        event.getMessage()
    //            .delete()
    //            .then(state.event.editReply(InteractionReplyEditSpec.builder()
    //                .addEmbed(EmbedCreateSpec.builder()
    //                    .title(state.name)
    //                    .addField("ID", state.id, false)
    //                    .addField("Bild", state.deploymentImage + " @ " + state.deploymentName,
    //                    false)
    //                    .description(String.join(" ", state.domains))
    //                    .color(Color.of(0, 102, 255))
    //                    .build())
    //                .addComponent(ActionRow.of(cancelButton, confirmButton))
    //                .build()))
    //            .subscribe();
    //      }
    //    });

    System.out.println("tihi?");
  }

  private Mono<Message> resetChannel(Snowflake channelId, Button createButton,
      Button deleteButton) {
    final var clearMessages = client.getChannelById(channelId)
        .ofType(GuildMessageChannel.class)
        .flatMapMany(channel -> {
          final var messagesBefore = channel.getMessagesBefore(Snowflake.of(Instant.now()));
          return messagesBefore.count().flatMapMany(count -> {
            if (count > 2) {
              return channel.bulkDeleteMessages(messagesBefore);
            }
            return messagesBefore.flatMap(Message::delete);
          });
        });

    final var sendNetworks = Mono.fromFuture(networkRepository.find(Query.empty()))
        .flatMap(networks -> client.getChannelById(channelId)
            .ofType(GuildMessageChannel.class)
            .flatMap(channel -> {
              final var builder = MessageCreateSpec.builder()
                  .content("Visar " + networks.values().size() + " nätverk:");

              networks.values().forEach(network -> {
                final var embedBuilder = EmbedCreateSpec.builder();

                embedBuilder.title(network.name().orElse("N/A"))
                    .description(network.id())
                    .addField("Deployments", "-", false);
                network.deployments()
                    .forEach(
                        (id, deployment) -> embedBuilder.addField(id, deployment.image(), true));
                embedBuilder.addField("Endpoints", "-", false);
                network.endpoints()
                    .forEach(
                        (domain, endpoint) -> embedBuilder.addField(domain.toString(), endpoint,
                            true));
                builder.addEmbed(embedBuilder.build());
              });

              return channel.createMessage(
                  builder.addComponent(ActionRow.of(createButton, deleteButton)).build());
            }));

    return clearMessages.then(sendNetworks);
  }

  private static class State {

    private final boolean creating;
    private final ButtonInteractionEvent event;
    private NetworkBuilder networkBuilder;

    public State(boolean creating, ButtonInteractionEvent event, NetworkBuilder networkBuilder) {
      this.creating = creating;
      this.event = event;
    }
  }
}
