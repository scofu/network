package com.scofu.network.instance;

import static com.scofu.network.instance.Domain.domain;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.scofu.common.json.Adapter;
import java.io.IOException;
import java.lang.reflect.Type;

final class DomainAdapter implements Adapter<Domain> {

  @Override
  public void write(Domain domain, JsonStream jsonStream, Type type) throws IOException {
    jsonStream.write('"');
    jsonStream.writeRaw("\\\"" + domain.string().replaceAll("\\.", "!") + "\\\"");
    jsonStream.write('"');
  }

  @Override
  public Domain read(JsonIterator jsonIterator, Type type) throws IOException {
    return domain(jsonIterator.readString().replaceAll("!", "."));
  }
}
