package com.scofu.network.instance;

import com.scofu.common.json.lazy.Lazy;
import com.scofu.network.document.Document;

/** The system. */
public interface System extends Lazy, Document {

  String ID = "system";

  String theme();

  void setTheme(String theme);
}
