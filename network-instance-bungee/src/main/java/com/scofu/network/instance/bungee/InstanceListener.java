package com.scofu.network.instance.bungee;

import static com.scofu.network.document.Filter.equalsTo;
import static com.scofu.network.document.Filter.where;
import static com.scofu.network.document.Query.query;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.document.DocumentStateListener;
import com.scofu.network.document.Query;
import com.scofu.network.instance.Instance;
import com.scofu.network.instance.InstanceRepository;
import com.scofu.network.instance.api.InstanceAvailabilityReply;
import com.scofu.network.instance.api.InstanceAvailabilityRequest;
import com.scofu.network.instance.api.InstanceConnectReply;
import com.scofu.network.instance.api.InstanceConnectRequest;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.Result;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.md_5.bungee.api.ProxyServer;

final class InstanceListener implements Feature {

  private final ProxyServer proxyServer;
  private final InstanceRepository instanceRepository;
  private final MessageQueue messageQueue;

  @Inject
  InstanceListener(
      MessageQueue messageQueue,
      MessageFlow messageFlow,
      ProxyServer proxyServer,
      InstanceRepository instanceRepository) {
    this.proxyServer = proxyServer;
    this.instanceRepository = instanceRepository;
    this.messageQueue = messageQueue;
    messageFlow
        .subscribeTo(InstanceConnectRequest.class)
        .replyWith(InstanceConnectReply.class)
        .withTopic("scofu.instance")
        .via(this::onInstanceConnectRequest);
    messageFlow
        .subscribeTo(InstanceAvailabilityRequest.class)
        .replyWith(InstanceAvailabilityReply.class)
        .withTopic("scofu.instance")
        .via(this::onInstanceAvailabilityRequest);
  }

  @Override
  public void enable() {
    instanceRepository
        .find(Query.empty())
        .map(Map::values)
        .accept(Collection::forEach, () -> (Consumer<Instance>) this::registerInstance);
    instanceRepository.addStateListener(
        new DocumentStateListener<>() {
          @Override
          public void onUpdate(Instance instance, boolean cached) {
            if (proxyServer.getServers().containsKey(instance.id())) {
              return;
            }
            registerInstance(instance);
          }

          @Override
          public void onDelete(String id) {
            proxyServer.getServers().remove(id);
          }
        });
  }

  private void registerInstance(Instance instance) {
    proxyServer
        .getServers()
        .put(
            instance.id(),
            proxyServer.constructServerInfo(instance.id(), instance.address(), null, false));
  }

  // TODO: move this to network instance service
  private Result<InstanceAvailabilityReply> onInstanceAvailabilityRequest(
      InstanceAvailabilityRequest request) {
    final var instances =
        instanceRepository
            .find(
                query().filter(where("deployment.id", equalsTo(request.deployment().id()))).build())
            .join()
            .values();

    record Pair(InstanceAvailabilityReply reply, Instance instance) {}

    final var availabilityResults =
        instances.stream()
            .map(
                instance ->
                    messageQueue
                        .declareFor(InstanceAvailabilityRequest.class)
                        .expectReply(InstanceAvailabilityReply.class)
                        .withTopic("scofu.instance." + instance.id())
                        .push(request)
                        .map(reply -> new Pair(reply, instance)));

    return Result.any(availabilityResults)
        .timeoutAfter(3, TimeUnit.SECONDS, () -> null)
        .map(
            pair -> {
              if (pair == null) {
                return new InstanceAvailabilityReply(true, null, null);
              }
              return pair.reply;
            });
  }

  private Result<InstanceConnectReply> onInstanceConnectRequest(InstanceConnectRequest request) {
    final var serverInfo = proxyServer.getServerInfo(request.instance().id());
    if (serverInfo == null) {
      return Result.of(
          new InstanceConnectReply(
              false, "No instance registered with id %s.".formatted(request.instance().id())));
    }
    for (var playerId : request.playerIds()) {
      final var player = proxyServer.getPlayer(playerId);
      if (player == null || !player.isConnected()) {
        return Result.of(
            new InstanceConnectReply(
                false, "Player with id %s is not online.".formatted(playerId)));
      }
      player.connect(serverInfo);
    }
    return Result.of(new InstanceConnectReply(true, null));
  }
}
