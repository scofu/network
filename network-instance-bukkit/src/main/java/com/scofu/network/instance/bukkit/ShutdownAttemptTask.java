package com.scofu.network.instance.bukkit;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.bukkit.event.ShutdownAttemptEvent;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

final class ShutdownAttemptTask implements Runnable, Feature {

  private final Plugin plugin;
  private final Server server;

  @Inject
  ShutdownAttemptTask(Plugin plugin, Server server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Override
  public void enable() {
    final var delay = 20 * 60; // 60 seconds
    server.getScheduler().runTaskTimerAsynchronously(plugin, this, delay, delay);
  }

  @Override
  public void run() {
    final var event = new ShutdownAttemptEvent();
    server.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return;
    }
    server
        .getScheduler()
        .runTask(
            plugin, () -> server.dispatchCommand(plugin.getServer().getConsoleSender(), "stop"));
  }
}
