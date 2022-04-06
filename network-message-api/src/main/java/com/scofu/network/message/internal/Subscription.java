package com.scofu.network.message.internal;

import com.google.inject.TypeLiteral;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

record Subscription<T, R>(TypeLiteral<T> type, TypeLiteral<R> replyType, Set<String> topics,
                          Function<? super T, CompletableFuture<? extends R>> function) {

  static <T, R> Subscription<T, R> of(TypeLiteral<T> type, TypeLiteral<R> replyType,
      Set<String> topics, Function<? super T, CompletableFuture<? extends R>> function) {
    return new Subscription<>(type, replyType, topics, function);
  }
}
