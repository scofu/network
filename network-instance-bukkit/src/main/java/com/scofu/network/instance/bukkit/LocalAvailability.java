package com.scofu.network.instance.bukkit;

import com.google.inject.Inject;
import com.scofu.network.instance.Availability;
import org.bukkit.Server;

final class LocalAvailability {

  private final Server server;
  private boolean available;
  private String status;

  @Inject
  LocalAvailability(Server server) {
    this.server = server;
    this.available = true;
  }

  public boolean available() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public String status() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Availability get() {
    final var full = server.getOnlinePlayers().size() >= server.getMaxPlayers();
    return new Availability(available, status == null && full ? "full" : status, full);
  }
}
