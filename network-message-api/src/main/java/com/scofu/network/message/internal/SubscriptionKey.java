package com.scofu.network.message.internal;

import com.google.inject.TypeLiteral;
import com.scofu.network.message.Result;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

record SubscriptionKey<T, R>(TypeLiteral<T> type, TypeLiteral<R> replyType, Set<String> topics,
                             Function<? super T, Result<? extends R>> function) {

  static <T, R> SubscriptionKey<T, R> of(TypeLiteral<T> type, TypeLiteral<R> replyType,
      Set<String> topics, Function<? super T, Result<? extends R>> function) {
    return new SubscriptionKey<>(type, replyType, topics, function);
  }
}
