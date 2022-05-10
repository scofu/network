package com.scofu.network.instance;

import com.scofu.common.json.lazy.Lazy;
import com.scofu.network.document.Document;
import java.net.InetSocketAddress;

/**
 * An instance.
 */
public interface Instance extends Lazy, Document {

  Deployment deployment();

  InetSocketAddress address();

  void setAddress(InetSocketAddress address);

  int playerCount();

  int incrementPlayerCount();

  int decrementPlayerCount();

  void setPlayerCount(int playerCount);

}
