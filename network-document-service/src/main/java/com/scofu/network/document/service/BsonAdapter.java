package com.scofu.network.document.service;

import com.google.common.reflect.TypeToken;
import com.google.inject.internal.MoreTypes;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.scofu.common.json.Adapter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

final class BsonAdapter implements Adapter<Bson> {

  @SuppressWarnings("UnstableApiUsage")
  private static final Type TYPE = new TypeToken<Map<String, Object>>() {}.getType();

  @Override
  public void write(Bson bson, JsonStream jsonStream, Type type) throws IOException {
    if (!(bson instanceof Map map)) {
      throw new IllegalArgumentException(
          String.format("Don't know how to write non-maplike bson object: %s (%s)", bson,
              MoreTypes.typeToString(type)));
    }
    jsonStream.writeVal(TYPE, map);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Bson read(JsonIterator jsonIterator, Type type) throws IOException {
    final var map = (Map<String, Object>) jsonIterator.read(TYPE);
    return new Document(map);
  }
}
