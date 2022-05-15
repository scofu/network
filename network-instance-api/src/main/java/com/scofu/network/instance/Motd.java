package com.scofu.network.instance;

import com.scofu.common.json.lazy.Lazy;
import net.kyori.adventure.text.Component;

/** Message of the day. */
public interface Motd extends Lazy {

  Component top();

  Component bottom();
}
