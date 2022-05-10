package com.scofu.network.instance.bungee;

import static com.scofu.network.instance.bungee.BungeeComponents.fromAdventure;
import static com.scofu.text.ContextualizedComponent.error;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.FinalEndpointResolver;
import com.scofu.network.instance.InstanceRepository;
import com.scofu.text.ThemeRegistry;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing.PlayerInfo;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerKickEvent.State;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Network listener.
 */
public final class NetworkListener implements Listener, Feature {

  private final InstanceRepository instanceRepository;
  private final ProxyServer proxyServer;
  private final FinalEndpointResolver finalEndpointResolver;
  private final ThemeRegistry themeRegistry;

  @Inject
  NetworkListener(InstanceRepository instanceRepository, ProxyServer proxyServer,
      FinalEndpointResolver finalEndpointResolver, ThemeRegistry themeRegistry) {
    this.instanceRepository = instanceRepository;
    this.proxyServer = proxyServer;
    this.finalEndpointResolver = finalEndpointResolver;
    this.themeRegistry = themeRegistry;
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
    final var serverConnectEvent = new ServerConnectEvent(event.getPlayer(),
        null,
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

    final var deployment = finalEndpointResolver.resolveDeployment(virtualHost).join().orElse(null);

    if (deployment == null) {
      event.getPlayer()
          .disconnect(fromAdventure(error().text("Couldn't resolve endpoint %s.",
              virtualHost.getHostString()).render(themeRegistry.byName("Vanilla").orElseThrow())));
      event.setCancelled(true);
      return;
    }

    if (event.getReason() == Reason.LOBBY_FALLBACK) {
      // TODO: make sure the failed request was to the default deployment instance
      sendPlayerThroughGatewayForDeployment(event, deployment);
      return;
    }

    final var availabilityReply = instanceRepository.checkAvailability(deployment,
        Map.of("slots", 1)).join();
    if (!availabilityReply.ok()) {
      event.getPlayer()
          .disconnect(fromAdventure(error().text("Availability error: %s",
              virtualHost.getHostString()).render(themeRegistry.byName("Vanilla").orElseThrow())));
      event.setCancelled(true);
      return;
    }

    if (availabilityReply.instance() == null) {
      sendPlayerThroughGatewayForDeployment(event, deployment);
      return;
    }

    event.setTarget(proxyServer.getServerInfo(availabilityReply.instance().id()));
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onProxyPingEvent(ProxyPingEvent event) {
    event.getResponse().setPlayers(new Players(0, 0, new PlayerInfo[0]));
    event.getResponse().setVersion(new Protocol("§r", -1337));
    final var virtualHost = event.getConnection().getVirtualHost();
    if (virtualHost == null) {
      System.out.println("null virtualhost! " + event.getConnection());
      return;
    }
    System.out.println("ping: " + virtualHost.getHostString());
    final var motd = finalEndpointResolver.resolveMotd(virtualHost).join().orElse(null);
    if (motd == null) {
      event.getResponse().setDescriptionComponent(new TextComponent(":'("));
      return;
    }
    event.getResponse()
        .setDescriptionComponent(fromAdventure(motd.top(), Component.newline(), motd.bottom()));
  }

  private void sendPlayerThroughGatewayForDeployment(ServerConnectEvent event,
      Deployment deployment) {
    event.setTarget(proxyServer.getServerInfo("gateway"));
    instanceRepository.deploy(deployment).thenAcceptAsync(reply -> {
      final var player = proxyServer.getPlayer(event.getPlayer().getUniqueId());
      if (!reply.ok()) {
        System.out.println("deploy error: " + reply.error());
        player.sendMessage(fromAdventure(error().text("Error: %s.", reply.error())
            .render(themeRegistry.byName("Vanilla").orElseThrow())));
        return;
      }
      if (!player.isConnected()) {
        System.out.println("player left! :(");
        return;
      }
      player.connect(proxyServer.getServerInfo(reply.instance().id()));
    });
  }

}
