package com.scofu.network.instance.bukkit;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.bukkit.event.ShutdownAttemptEvent;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

final class BasicShutdownAttemptListener implements Listener, Feature {

  private final Server server;

  @Inject
  BasicShutdownAttemptListener(Server server) {
    this.server = server;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  private void onShutdownAttemptEvent(ShutdownAttemptEvent event) {
    if (server.getOnlinePlayers().size() != 0) {
      event.setCancelled(true);
    }
  }
}
