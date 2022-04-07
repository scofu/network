package com.scofu.network.instance.bukkit;

import com.scofu.common.inject.Feature;
import com.scofu.common.json.Json;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Group;
import com.scofu.network.instance.GroupRepository;
import java.net.InetAddress;
import javax.inject.Inject;
import javax.inject.Named;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class GroupListener implements Listener, Feature {

  private final GroupRepository groupRepository;
  private final Json json;
  private final InetAddress localHost;

  @Inject
  GroupListener(GroupRepository groupRepository, Json json,
      @Named("LocalHost") InetAddress localHost) {
    this.groupRepository = groupRepository;
    this.json = json;
    this.localHost = localHost;
  }

  @EventHandler
  private void onPlayerJoinEvent(PlayerJoinEvent event) {
    final var deployment = json.fromString(Deployment.class, System.getenv("INSTANCE_DEPLOYMENT"));
    final var group = groupRepository.byId(deployment.groupId())
        .orElse(new Group(deployment.groupId()));
    group.instancePlayerCountMap()
        .put(localHost.getHostName(), event.getPlayer().getServer().getOnlinePlayers().size());
    groupRepository.update(group);
    System.out.println(
        "group:" + group.id() + " - " + event.getPlayer().getServer().getOnlinePlayers().size());
  }

  @EventHandler
  private void onPlayerQuitEvent(PlayerQuitEvent event) {
    final var deployment = json.fromString(Deployment.class, System.getenv("INSTANCE_DEPLOYMENT"));
    final var group = groupRepository.byId(deployment.groupId()).orElse(null);
    if (group == null) {
      return;
    }
    group.instancePlayerCountMap()
        .put(localHost.getHostName(), event.getPlayer().getServer().getOnlinePlayers().size() - 1);
    groupRepository.update(group);
    System.out.println(
        "group:" + group.id() + " - " + (event.getPlayer().getServer().getOnlinePlayers().size()
            - 1));
  }

}
