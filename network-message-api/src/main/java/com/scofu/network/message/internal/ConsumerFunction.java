package com.scofu.network.message.internal;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
interface ConsumerFunction<T, R> extends Consumer<T>, Function<T, CompletableFuture<? extends R>> {

  static <T, R> ConsumerFunction<T, R> wrap(Consumer<? super T> consumer) {
    return consumer::accept;
  }

  @Override
  default CompletableFuture<R> apply(T t) {
    accept(t);
    return completedFuture(null);
  }
}
