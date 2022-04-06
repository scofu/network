package com.scofu.network.document.internal;

import com.google.common.collect.ImmutableMap;
import com.scofu.network.document.Filter;
import java.util.Map;

/**
 * Internal filter.
 */
public class InternalFilter implements Filter {

  private final Map<String, Object> map;

  private InternalFilter(Map<String, Object> map) {
    this.map = map;
  }

  public static InternalFilter newInternalFilter(Map<String, Object> map) {
    return new InternalFilter(map);
  }

  public Map<String, Object> map() {
    return map;
  }

  @Override
  public Filter and(Filter filter) {
    if (!(filter instanceof InternalFilter internalFilter)) {
      throw new UnsupportedOperationException("Unknown filter type: " + filter);
    }
    return new InternalFilter(
        ImmutableMap.<String, Object>builder().putAll(map).putAll(internalFilter.map).build());
  }

  @Override
  public Map<String, Object> asMap() {
    return map;
  }
}
