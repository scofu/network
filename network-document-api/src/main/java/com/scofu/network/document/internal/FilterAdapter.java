package com.scofu.network.document.internal;

import com.google.common.reflect.TypeToken;
import com.google.inject.internal.MoreTypes;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.scofu.common.json.Adapter;
import com.scofu.network.document.Filter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

final class FilterAdapter implements Adapter<Filter> {

  @SuppressWarnings("UnstableApiUsage")
  private static final Type TYPE = new TypeToken<Map<String, Object>>() {}.getType();

  @Override
  public void write(Filter filter, JsonStream jsonStream, Type type) throws IOException {
    if (!(filter instanceof InternalFilter internalFilter)) {
      throw new UnsupportedOperationException(
          "Unknown filter type: " + MoreTypes.typeToString(type));
    }
    jsonStream.writeVal(TYPE, internalFilter.map());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Filter read(JsonIterator jsonIterator, Type type) throws IOException {
    return InternalFilter.newInternalFilter((Map<String, Object>) jsonIterator.read(TYPE));
  }
}
