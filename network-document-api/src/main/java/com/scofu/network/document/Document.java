package com.scofu.network.document;

import com.jsoniter.annotation.JsonProperty;

/**
 * An identifiable and queryable document.
 */
public interface Document extends Identifiable<String> {

  @Override
  @JsonProperty("_id")
  String id();

}
