package com.scofu.network.instance;

import com.scofu.common.json.lazy.Lazy;
import java.util.Map;

/**
 * A deployment.
 */
public interface Deployment extends Lazy {

  String id();

  String image();

  String name();

  Map<String, String> environment();

}
