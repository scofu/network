package com.scofu.network.document.lazy;

import com.google.inject.Inject;
import com.google.inject.internal.MoreTypes;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.scofu.common.json.Adapter;
import java.io.IOException;
import java.lang.reflect.Type;

final class LazyDocumentAdapter implements Adapter<LazyDocument> {

  private final LazyDocumentFactory lazyDocumentFactory;

  @Inject
  LazyDocumentAdapter(LazyDocumentFactory lazyDocumentFactory) {
    this.lazyDocumentFactory = lazyDocumentFactory;
  }

  @Override
  public void write(LazyDocument value, JsonStream jsonStream, Type type) throws IOException {
    jsonStream.writeVal(value.any());
  }

  @SuppressWarnings("unchecked")
  @Override
  public LazyDocument read(JsonIterator jsonIterator, Type type) throws IOException {
    final var any = jsonIterator.readAny();
    final var id = any.get("_id").as(String.class);
    return lazyDocumentFactory.create((Class<LazyDocument>) MoreTypes.getRawType(type), id, any);
  }
}
