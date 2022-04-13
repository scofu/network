package com.scofu.network.instance;

import com.google.common.collect.Maps;
import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;
import com.scofu.network.document.Document;
import java.util.Map;

/**
 * Represents a group.
 */
public class Group implements Document {

  @JsonProperty("_id")
  private final String id;
  private final Map<String, Integer> instancePlayerCountMap;

  @JsonCreator
  public Group(@JsonProperty("_id") String id) {
    this.id = id;
    this.instancePlayerCountMap = Maps.newLinkedHashMap();
  }

  @Override
  public String id() {
    return id;
  }

  /**
   * Returns the instance player count map.
   */
  public Map<String, Integer> instancePlayerCountMap() {
    return instancePlayerCountMap;
  }

  /**
   * Returns the total player count.
   */
  public int playerCount() {
    return instancePlayerCountMap.values().stream().mapToInt(x -> x).sum();
  }
}
