package com.scofu.network.message.internal;

import com.google.inject.TypeLiteral;

record Queue<T, R>(TypeLiteral<T> type, TypeLiteral<R> replyType, String topic) {

  static <T, R> Queue<T, R> of(TypeLiteral<T> type, TypeLiteral<R> replyType, String topic) {
    return new Queue<>(type, replyType, topic);
  }
}
