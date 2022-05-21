package com.scofu.network.instance;

import com.scofu.common.PeriodEscapedString;
import com.scofu.common.json.lazy.Lazy;
import com.scofu.network.document.Document;
import java.util.Map;

/** An endpoint. */
public interface Network extends Lazy, Document {

  Map<PeriodEscapedString, Deployment> deployments();
}
