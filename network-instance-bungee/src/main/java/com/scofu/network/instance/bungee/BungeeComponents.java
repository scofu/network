package com.scofu.network.instance.bungee;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

/** Bungee component utils. */
public class BungeeComponents {

  private BungeeComponents() {}

  /**
   * Converts the given components to a base component.
   *
   * @param component the component
   * @param extra the extra
   */
  public static BaseComponent fromAdventure(Component component, Component... extra) {
    if (extra == null || extra.length == 0) {
      return new TextComponent(
          ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(component)));
    }
    var builder = new ComponentBuilder();
    builder =
        builder.append(
            ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(component)));
    for (var extraComponent : extra) {
      builder =
          builder.append(
              ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(extraComponent)));
    }
    return new TextComponent(builder.create());
  }
}
