package com.scofu.network.instance.bungee;

import com.google.common.collect.Sets;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.bungee.protocol.ExposedProtocol;
import com.scofu.network.instance.bungee.protocol.ResourcePackSendPacket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

/** Resource Pack Listener. */
public final class ResourcePackListener implements Listener, Feature {

  private static final String URL =
      "https://repo.scofu.com/" + "repository/assets/resourcepacks/scofu.zip";
  private static final String HASH = "63b0f8475cf73a58497936852b95bcf1e09126f0";

  private final ProxyServer proxyServer;
  private final Plugin plugin;
  private final Set<UUID> playersWithResourcePack;

  @Inject
  ResourcePackListener(ProxyServer proxyServer, Plugin plugin) {
    this.proxyServer = proxyServer;
    this.plugin = plugin;
    this.playersWithResourcePack = Sets.newConcurrentHashSet();
    ExposedProtocol.registerToClientPacket(
        ResourcePackSendPacket.class, ResourcePackSendPacket::new, 60);
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onServerConnectedEvent(ServerConnectedEvent event) {
    if (playersWithResourcePack.contains(event.getPlayer().getUniqueId())) {
      return;
    }
    System.out.println("sending resource pack!");
    proxyServer
        .getScheduler()
        .schedule(
            plugin,
            () -> {
              if (!event.getPlayer().isConnected()) {
                return;
              }
              playersWithResourcePack.add(event.getPlayer().getUniqueId());
              event
                  .getPlayer()
                  .unsafe()
                  .sendPacket(new ResourcePackSendPacket(URL, HASH, true, "{\"text\":\"test\"}"));
            },
            1,
            TimeUnit.SECONDS);
  }

  /**
   * Event.
   *
   * @param event the event
   */
  @EventHandler
  public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
    playersWithResourcePack.remove(event.getPlayer().getUniqueId());
  }
}
