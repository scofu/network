package com.scofu.network.instance.bungee;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.api.InstanceGoodbyeMessage;
import com.scofu.network.instance.api.InstanceHelloMessage;
import com.scofu.network.instance.api.InstanceLookupReply;
import com.scofu.network.instance.api.InstanceLookupRequest;
import com.scofu.network.instance.api.InstanceNavigateReply;
import com.scofu.network.instance.api.InstanceNavigateRequest;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.QueueBuilder;
import java.util.concurrent.CompletableFuture;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;

final class InstanceListener implements Feature {

  private final ProxyServer proxyServer;
  private final QueueBuilder<InstanceLookupRequest, InstanceLookupReply> lookupQueue;

  @Inject
  InstanceListener(MessageQueue messageQueue, MessageFlow messageFlow, ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
    this.lookupQueue = messageQueue.declareFor(InstanceLookupRequest.class)
        .expectReply(InstanceLookupReply.class)
        .withTopic("scofu.instance.lookup");
    messageFlow.subscribeTo(InstanceHelloMessage.class)
        .withTopic("scofu.instance.hello")
        .via(this::handleHello);
    messageFlow.subscribeTo(InstanceGoodbyeMessage.class)
        .withTopic("scofu.instance.goodbye")
        .via(this::handleGoodbye);
    messageFlow.subscribeTo(InstanceNavigateRequest.class)
        .replyWith(InstanceNavigateReply.class)
        .withTopic("scofu.instance.navigate")
        .via(this::handleNavigate);
  }

  @Override
  public void enable() {
    lookupQueue.push(new InstanceLookupRequest()).whenComplete((reply, throwable) -> {
      if (throwable != null) {
        throwable.printStackTrace();
        return;
      }
      reply.instances()
          .forEach(instance -> proxyServer.getServers()
              .put(instance.id(),
                  proxyServer.constructServerInfo(instance.id(), instance.address(), null, false)));
    });
  }

  private CompletableFuture<InstanceNavigateReply> handleNavigate(InstanceNavigateRequest request) {
    final var serverInfo = proxyServer.getServerInfo(request.instanceId());
    if (serverInfo == null) {
      return CompletableFuture.completedFuture(new InstanceNavigateReply(false,
          "No instance registered with id " + request.instanceId() + "."));
    }
    final var player = proxyServer.getPlayer(request.id());
    if (player == null || !player.isConnected()) {
      return CompletableFuture.completedFuture(
          new InstanceNavigateReply(false, "Player not online."));
    }
    final var future = new CompletableFuture<InstanceNavigateReply>();
    player.connect(serverInfo, (ok, throwable) -> {
      if (!ok) {
        if (throwable != null) {
          throwable.printStackTrace();
        }
        future.complete(new InstanceNavigateReply(false, "Proxy connect exception."));
        return;
      }
      future.complete(new InstanceNavigateReply(true, null));
    }, Reason.PLUGIN);
    return future;
  }

  private void handleHello(InstanceHelloMessage message) {
    proxyServer.getServers()
        .put(message.instance().id(),
            proxyServer.constructServerInfo(message.instance().id(), message.instance().address(),
                null, false));
    System.out.println(
        "registered server: " + proxyServer.getServers().get(message.instance().id()));
  }

  private void handleGoodbye(InstanceGoodbyeMessage message) {
    proxyServer.getServers().remove(message.instance().id());
  }
}
