package com.scofu.network.instance.bukkit;

import com.scofu.common.inject.Feature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;

final class TestListener implements Listener, Feature {

  @EventHandler
  private void onPlayerJoinEvent(PlayerJoinEvent event) {
    event.getPlayer().sendMessage("yoo2");
    //    event.getPlayer().updateCommands();
  }

  @EventHandler
  private void onPlayerCommandSendEvent(PlayerCommandSendEvent event) {
    event.getPlayer().sendMessage("event!");
  }

}
