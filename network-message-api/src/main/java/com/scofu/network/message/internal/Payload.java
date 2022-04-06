package com.scofu.network.message.internal;

import com.scofu.common.json.DynamicReference;
import javax.annotation.Nullable;

record Payload(@Nullable String id, @Nullable DynamicReference<?> message,
               @Nullable DynamicReference<?> reply) {

  static Payload of(@Nullable String id, @Nullable DynamicReference<?> message,
      @Nullable DynamicReference<?> reply) {
    return new Payload(id, message, reply);
  }
}
