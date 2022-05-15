package com.scofu.network.instance.bukkit.event;

import java.util.Map;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Availability check event. */
public class AvailabilityCheckEvent extends Event implements Cancellable {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final Map<String, Object> context;
  private boolean cancel = false;

  public AvailabilityCheckEvent(Map<String, Object> context) {
    super(true);
    this.context = context;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }

  @Override
  public void setCancelled(boolean b) {
    cancel = b;
  }

  public Map<String, Object> context() {
    return context;
  }
}
