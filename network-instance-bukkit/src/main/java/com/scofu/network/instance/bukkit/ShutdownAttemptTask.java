package com.scofu.network.instance.bukkit;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.bukkit.event.ShutdownAttemptEvent;
import org.bukkit.plugin.Plugin;

final class ShutdownAttemptTask implements Runnable, Feature {

  private final Plugin plugin;

  @Inject
  ShutdownAttemptTask(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void enable() {
    final var delay = 20 * 60; // 60 seconds
    plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, delay, delay);
  }

  @Override
  public void run() {
    final var event = new ShutdownAttemptEvent();
    plugin.getServer().getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return;
    }
    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "stop");
  }
}
