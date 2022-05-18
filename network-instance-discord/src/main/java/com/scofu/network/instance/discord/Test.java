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
import com.scofu.network.message.Result;
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
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import net.renfei.cloudflare.Cloudflare;
import reactor.core.publisher.Flux;
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
    client
        .updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.watching("www.scofu.com")))
        .block();
    System.out.println("yo!");
    Stream.of("test", "build", "testing")
        .map(networkRepository::delete)
        .collect(Result.toResult())
        .flatMap(unused -> networkRepository.find(Query.empty()))
        .map(Map::values)
        .filter(Collection::isEmpty)
        .flatMap(
            networks -> {
              final var buildDeployment =
                  lazyFactory.create(
                      Deployment.class,
                      Map.of(
                          Deployment::id,
                          "build",
                          Deployment::image,
                          "docker.scofu.com/bukkit-build:latest",
                          Deployment::name,
                          "build",
                          Deployment::environment,
                          Map.of()));
              final var lobbyDeployment =
                  lazyFactory.create(
                      Deployment.class,
                      Map.of(
                          Deployment::id,
                          "lobby",
                          Deployment::image,
                          "docker.scofu.com/lobby:latest",
                          Deployment::name,
                          "lobby",
                          Deployment::environment,
                          Map.of()));
              return networkRepository.update(
                  lazyFactory.create(
                      Network.class,
                      Network::id,
                      "testing",
                      Network::deployments,
                      Map.of(
                          new PeriodEscapedString("build.scofu.com"),
                          buildDeployment,
                          new PeriodEscapedString("scofu.com"),
                          lobbyDeployment)));
            })
        .accept(
            unused -> {
              final var channelId = Snowflake.of("905351244204367894");
              client
                  .on(GuildCreateEvent.class)
                  .subscribe(
                      event -> {
                        System.out.println("guild create!!");
                        resetChannel(channelId).subscribe();
                      });
            });

    final var guildId = Snowflake.of("905343563930427472");
    final var confirmButton = Button.success("network-confirm", "Bekräfta");
    final var cancelButton = Button.danger("network-cancel", "Avbryt");
  }

  private Mono<Message> resetChannel(Snowflake channelId) {
    final var channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class);
    final var networks = Mono.fromFuture(networkRepository.find(Query.empty()).unbox());
    final var message = networks.flatMap(map -> createNetworksMessage(channel, map.values()));
    return clearMessages(channel).then(message);
  }

  private Mono<Message> createNetworksMessage(
      Mono<GuildMessageChannel> channelMono, Collection<Network> networks) {
    final var size = networks.size();
    final var builder =
        MessageCreateSpec.builder()
            .content("Showing %s network%s:".formatted(size, size == 1 ? "" : " "));
    for (var network : networks) {
      final var embedBuilder =
          EmbedCreateSpec.builder()
              .title(network.id())
              .description("...")
              .addField("Deployments", "-", false);
      network
          .deployments()
          .forEach(
              (id, deployment) ->
                  embedBuilder.addField(Periods.unescape(id.toString()), deployment.image(), true));
      builder.addEmbed(embedBuilder.build());
    }
    final var createButton = Button.primary("network-create", "Skapa ett nätverk");
    final var deleteButton = Button.primary("network-delete", "Ta bort ett nätverk");
    return channelMono.flatMap(
        channel ->
            channel.createMessage(
                builder.addComponent(ActionRow.of(createButton, deleteButton)).build()));
  }

  private Flux<Object> clearMessages(Mono<GuildMessageChannel> channelMono) {
    return channelMono.flatMapMany(
        channel -> {
          final var before = channel.getMessagesBefore(Snowflake.of(Instant.now()));
          return before
              .count()
              .flatMapMany(
                  count ->
                      count > 2
                          ? channel.bulkDeleteMessages(before)
                          : before.flatMap(Message::delete));
        });
  }
}
