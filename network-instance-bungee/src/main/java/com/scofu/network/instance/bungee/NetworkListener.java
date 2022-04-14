package com.scofu.network.instance.bungee;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Network;
import com.scofu.network.instance.NetworkRepository;
import com.scofu.network.instance.api.InstanceAvailabilityReply;
import com.scofu.network.instance.api.InstanceAvailabilityRequest;
import com.scofu.network.instance.api.InstanceDeployReply;
import com.scofu.network.instance.api.InstanceDeployRequest;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.QueueBuilder;
import java.net.InetSocketAddress;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing.PlayerInfo;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerKickEvent.State;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Network listener.
 */
public final class NetworkListener implements Listener, Feature {

  private final NetworkRepository networkRepository;
  private final MessageQueue messageQueue;
  private final ProxyServer proxyServer;
  private final QueueBuilder<InstanceDeployRequest, InstanceDeployReply> deployQueue;
  private QueueBuilder<InstanceAvailabilityRequest, InstanceAvailabilityReply> availabilityQueue;

  @Inject
  NetworkListener(NetworkRepository networkRepository, MessageQueue messageQueue,
      ProxyServer proxyServer) {
    this.networkRepository = networkRepository;
    this.messageQueue = messageQueue;
    this.proxyServer = proxyServer;
    this.availabilityQueue = messageQueue.declareFor(InstanceAvailabilityRequest.class)
        .expectReply(InstanceAvailabilityReply.class)
        .withTopic("scofu.instance.availability");
    this.deployQueue = messageQueue.declareFor(InstanceDeployRequest.class)
        .expectReply(InstanceDeployReply.class)
        .withTopic("scofu.instance.deploy");
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onLoginEvent(LoginEvent event) {
    System.out.println("login domain: " + event.getConnection().getVirtualHost().getHostString());
    System.out.println("login domain: " + event.getConnection().getVirtualHost().getHostName());
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onPlayerHandshakeEvent(PlayerHandshakeEvent event) {
    System.out.println(
        "handshake domain: " + event.getConnection().getVirtualHost().getHostString());
    System.out.println("handshake domain: " + event.getConnection().getVirtualHost().getHostName());
    System.out.println("handshake host: " + event.getHandshake().getHost());
    System.out.println("handshake port: " + event.getHandshake().getPort());
    System.out.println(
        "handshake isa: " + InetSocketAddress.createUnresolved(event.getHandshake().getHost(),
            event.getHandshake().getPort()).getHostString());
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onServerKickEvent(ServerKickEvent event) {
    if (event.getKickedFrom().getName().equals("gateway")) {
      return;
    }
    if (event.getState() != State.CONNECTED) {
      return;
    }
    event.setCancelled(true);
    //    event.setCancelServer(proxyServer.getServerInfo("gateway"));
    final var serverConnectEvent = new ServerConnectEvent(event.getPlayer(), null,
        Reason.JOIN_PROXY);
    onServerConnectEvent(serverConnectEvent);
    event.setCancelServer(serverConnectEvent.getTarget());
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onServerConnectEvent(ServerConnectEvent event) {
    if (!event.getPlayer().getName().equals("Jezper")) {
      event.setCancelled(true);
      return;
    }

    System.out.println("CONNECT REASON: " + event.getReason());
    System.out.println("- REQUEST: " + event.getRequest());

    if (event.getReason() != Reason.JOIN_PROXY && event.getReason() != Reason.LOBBY_FALLBACK) {
      return;
    }

    final var player = event.getPlayer();
    final var virtualHost = player.getPendingConnection().getVirtualHost();
    final var domain = parseDomain(virtualHost.getHostString());

    System.out.println("domain: " + player.getName() + " - " + domain);

    final var network = networkRepository.findByDomain(domain).join().orElse(null);

    if (network == null) {
      System.out.println("unmapped endpoint: " + domain);
      event.getPlayer().disconnect("unmapped endpoint: " + domain);
      event.setCancelled(true);
      return;
    }

    final var deployment = network.deploymentByEndpoint(domain).orElse(null);

    if (deployment == null) {
      System.out.println("unknown endpoint: " + domain);
      event.getPlayer().disconnect("unknown endpoint: " + domain);
      event.setCancelled(true);
      return;
    }

    if (event.getReason() == Reason.LOBBY_FALLBACK) {
      // TODO: make sure the failed request was to the default deployment instance
      sendPlayerThroughGatewayForDeployment(event, network, deployment);
      return;
    }

    final var reply = availabilityQueue.push(
        new InstanceAvailabilityRequest(deployment.groupId(), Map.of("slots", 1))).join();
    if (!reply.ok()) {
      System.out.println("error: " + reply.error());
      event.setCancelled(true);
      return;
    }

    if (reply.instance() == null) {
      sendPlayerThroughGatewayForDeployment(event, network, deployment);
      return;
    }

    event.setTarget(proxyServer.getServerInfo(reply.instance().id()));
  }

  private String parseDomain(String hostString) {
    return hostString.replaceFirst("mc\\.", "");
  }

  private void sendPlayerThroughGatewayForDeployment(ServerConnectEvent event, Network network,
      Deployment deployment) {
    event.setTarget(proxyServer.getServerInfo("gateway"));
    deployQueue.push(new InstanceDeployRequest(network.id(), deployment))
        .whenComplete((deployReply, throwable) -> {
          if (throwable != null) {
            throwable.printStackTrace();
            return;
          }
          if (!deployReply.ok()) {
            System.out.println("deploy error: " + deployReply.error());
            return;
          }
          final var player = proxyServer.getPlayer(event.getPlayer().getUniqueId());
          if (!player.isConnected()) {
            System.out.println("player left! :(");
            return;
          }
          player.connect(proxyServer.getServerInfo(deployReply.instance().id()));
        });
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onProxyPingEvent(ProxyPingEvent event) {
    final var virtualHost = event.getConnection().getVirtualHost();
    final var domain = virtualHost.getHostString();
    System.out.println("ping domain: " + domain);
    event.getResponse()
        .setDescriptionComponent(new TextComponent(
            new ComponentBuilder("sᴏᴏɴ™" + Strings.repeat(" ", 46) + "§8...eller?").color(
                ChatColor.of("#0066ff")).create()));
    event.getResponse().setPlayers(new Players(0, 0, new PlayerInfo[0]));
    event.getResponse()
        .setVersion(new Protocol("§6scofu! §e⚡§7 " + Strings.repeat(" ", 60) + "⭐ (snart)", -1337));
  }

}
