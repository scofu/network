package com.scofu.network.instance.bukkit;

import com.scofu.common.inject.Feature;
import com.scofu.network.instance.InstanceRepository;
import javax.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class PlayerCountListener implements Listener, Feature {

  private final InstanceRepository instanceRepository;
  private final LocalInstanceProvider instanceProvider;

  @Inject
  PlayerCountListener(InstanceRepository instanceRepository,
      LocalInstanceProvider instanceProvider) {
    this.instanceRepository = instanceRepository;
    this.instanceProvider = instanceProvider;
  }

  @EventHandler
  private void onPlayerJoinEvent(PlayerJoinEvent event) {
    instanceProvider.get().thenComposeAsync(instance -> {
      instance.incrementPlayerCount();
      return instanceRepository.update(instance);
    });
  }

  @EventHandler
  private void onPlayerQuitEvent(PlayerQuitEvent event) {
    instanceProvider.get().thenComposeAsync(instance -> {
      instance.decrementPlayerCount();
      return instanceRepository.update(instance);
    });
  }

}
