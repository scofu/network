package com.scofu.network.instance.discord;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.PeriodEscapedString;
import com.scofu.common.json.Periods;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.document.Query;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Network;
import com.scofu.network.instance.NetworkRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import java.time.Instant;
import java.util.Map;
import net.renfei.cloudflare.Cloudflare;
import reactor.core.publisher.Mono;

final class Test implements Feature {

  private final GatewayDiscordClient client;
  private final NetworkRepository networkRepository;
  private final LazyFactory lazyFactory;
  private final Cloudflare cloudflare;

  @Inject
  Test(GatewayDiscordClient client, NetworkRepository networkRepository, LazyFactory lazyFactory) {
    this.client = client;
    this.networkRepository = networkRepository;
    this.lazyFactory = lazyFactory;
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
      final var deployment = lazyFactory.create(Deployment.class,
          Map.of(Deployment::id, "build", Deployment::image, "docker.scofu.com/bukkit-build:latest",
              Deployment::name, "build", Deployment::environment, Map.of()));

      networkRepository.update(
          lazyFactory.create(Network.class, Network::id, "build", Network::deployments,
              Map.of(new PeriodEscapedString("build.scofu.com"), deployment))).join();
    }

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

                embedBuilder.title(network.id())
                    .description("...")
                    .addField("Deployments", "-", false);
                network.deployments()
                    .forEach(
                        (id, deployment) -> embedBuilder.addField(Periods.unescape(id.toString()),
                            deployment.image(), true));
                builder.addEmbed(embedBuilder.build());
              });

              return channel.createMessage(
                  builder.addComponent(ActionRow.of(createButton, deleteButton)).build());
            }));

    return clearMessages.then(sendNetworks);
  }
}
