package com.scofu.network.document.lazy;

import com.jsoniter.any.Any;
import com.scofu.network.document.Document;

/**
 * Represents a lazy document.
 */
public interface LazyDocument extends Document {

  /**
   * Returns the internal json representation.
   */
  Any any();

}
