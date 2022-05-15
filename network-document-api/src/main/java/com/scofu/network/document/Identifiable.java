package com.scofu.network.document;

import com.jsoniter.annotation.JsonProperty;

/**
 * Something identifiable.
 *
 * @param <T> the type of the identifier
 */
public interface Identifiable<T> {

  /** Returns the identifier. */
  @JsonProperty("_id")
  T id();
}
