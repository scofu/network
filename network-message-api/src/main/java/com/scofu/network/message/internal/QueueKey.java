package com.scofu.network.message.internal;

import com.google.inject.TypeLiteral;

record QueueKey<T, R>(TypeLiteral<T> type, TypeLiteral<R> replyType, String topic) {

  static <T, R> QueueKey<T, R> of(TypeLiteral<T> type, TypeLiteral<R> replyType, String topic) {
    return new QueueKey<>(type, replyType, topic);
  }
}
