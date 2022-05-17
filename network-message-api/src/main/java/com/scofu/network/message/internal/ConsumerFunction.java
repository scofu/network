package com.scofu.network.message.internal;

import com.scofu.network.message.Result;
import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
interface ConsumerFunction<T, R> extends Consumer<T>, Function<T, Result<? extends R>> {

  static <T, R> ConsumerFunction<T, R> wrap(Consumer<? super T> consumer) {
    return consumer::accept;
  }

  @Override
  default Result<R> apply(T t) {
    accept(t);
    return Result.empty();
  }
}
