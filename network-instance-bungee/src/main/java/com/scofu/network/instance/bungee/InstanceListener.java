package com.scofu.network.instance.bungee;

import static com.scofu.network.document.Filter.equalsTo;
import static com.scofu.network.document.Filter.where;
import static java.util.concurrent.CompletableFuture.completedFuture;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;

final class InstanceListener implements Feature {

  private final ProxyServer proxyServer;
  private final InstanceRepository instanceRepository;
  private final MessageQueue messageQueue;

  @Inject
  InstanceListener(MessageQueue messageQueue, MessageFlow messageFlow, ProxyServer proxyServer,
      InstanceRepository instanceRepository) {
    this.proxyServer = proxyServer;
    this.instanceRepository = instanceRepository;
    this.messageQueue = messageQueue;
    messageFlow.subscribeTo(InstanceConnectRequest.class)
        .replyWith(InstanceConnectReply.class)
        .withTopic("scofu.instance")
        .via(this::onInstanceConnectRequest);
    messageFlow.subscribeTo(InstanceAvailabilityRequest.class)
        .replyWith(InstanceAvailabilityReply.class)
        .withTopic("scofu.instance")
        .via(this::onInstanceAvailabilityRequest);
  }

  @Override
  public void enable() {
    instanceRepository.find(Query.empty())
        .thenAccept(map -> map.values()
            .forEach(instance -> proxyServer.getServers()
                .put(instance.id(),
                    proxyServer.constructServerInfo(instance.id(),
                        instance.address(),
                        null,
                        false))));
    instanceRepository.addStateListener(new DocumentStateListener<>() {
      @Override
      public void onUpdate(Instance instance, boolean cached) {
        if (proxyServer.getServers().containsKey(instance.id())) {
          return;
        }
        proxyServer.getServers()
            .put(instance.id(),
                proxyServer.constructServerInfo(instance.id(), instance.address(), null, false));
      }

      @Override
      public void onDelete(String id) {
        proxyServer.getServers().remove(id);
      }
    });
  }

  // TODO: move this to network instance service
  private CompletableFuture<InstanceAvailabilityReply> onInstanceAvailabilityRequest(
      InstanceAvailabilityRequest request) {
    final var future = new CompletableFuture<InstanceAvailabilityReply>();
    final var instances = instanceRepository.find(Query.builder()
        .filter(where("deployment.id", equalsTo(request.deployment().id())))
        .build()).join().values();

    record Reply(InstanceAvailabilityReply reply, Instance instance) {}

    CompletableFuture.anyOf(instances.stream()
            .map(instance -> messageQueue.declareFor(InstanceAvailabilityRequest.class)
                .expectReply(InstanceAvailabilityReply.class)
                .withTopic("scofu.instance." + instance.id())
                .push(request)
                .thenApply(reply -> new Reply(reply, instance)))
            .toArray(CompletableFuture[]::new))
        .completeOnTimeout(null, 5, TimeUnit.SECONDS)
        .whenCompleteAsync((reply, throwable) -> {
          if (throwable != null) {
            throwable.printStackTrace();
            future.complete(new InstanceAvailabilityReply(false, "Service exception.", null));
            return;
          }
          if (reply == null) {
            future.complete(new InstanceAvailabilityReply(true, null, null));
            return;
          }
          future.complete(((Reply) reply).reply);
        });
    return future;
  }

  private CompletableFuture<InstanceConnectReply> onInstanceConnectRequest(
      InstanceConnectRequest request) {
    final var serverInfo = proxyServer.getServerInfo(request.instance().id());
    if (serverInfo == null) {
      return completedFuture(new InstanceConnectReply(false,
          "No instance registered with id %s.".formatted(request.instance().id())));
    }
    for (var playerId : request.playerIds()) {
      final var player = proxyServer.getPlayer(playerId);
      if (player == null || !player.isConnected()) {
        return completedFuture(new InstanceConnectReply(false,
            "Player with id %s is not online.".formatted(playerId)));
      }
      player.connect(serverInfo);
    }
    return completedFuture(new InstanceConnectReply(true, null));
  }
}
