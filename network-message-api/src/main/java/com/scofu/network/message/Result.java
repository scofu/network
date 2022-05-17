package com.scofu.network.message;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A future result.
 *
 * @param <T> the type of the result.
 */
public class Result<T> {

  private final CompletableFuture<T> completableFuture;
  private final Executor executor;
  private final AtomicReference<List<Runnable>> timeoutListeners;

  Result(
      CompletableFuture<T> completableFuture,
      Executor executor,
      AtomicReference<List<Runnable>> timeoutListeners) {
    this.completableFuture = completableFuture;
    this.executor = executor;
    this.timeoutListeners = timeoutListeners;
  }

  public static <T> Collector<Result<T>, ?, Result<Stream<T>>> toResult() {
    return Collectors.collectingAndThen(Collectors.<Result<T>>toUnmodifiableList(), Result::all);
  }

  /**
   * Wraps and returns a new result around the given completable future.
   *
   * @param completableFuture the completable future
   * @param executor the executor
   * @param <T> the type of the result
   */
  public static <T> Result<T> of(CompletableFuture<T> completableFuture, Executor executor) {
    return new Result<>(completableFuture, executor, new AtomicReference<>());
  }

  /**
   * Wraps and returns a new result around the given completable future.
   *
   * @param completableFuture the completable future
   * @param <T> the type of the result
   */
  public static <T> Result<T> of(CompletableFuture<T> completableFuture) {
    return new Result<>(
        completableFuture, completableFuture.defaultExecutor(), new AtomicReference<>());
  }

  /**
   * See {@link CompletableFuture#supplyAsync(Supplier, Executor)}.
   *
   * @param supplier the supplier
   * @param executor the executor
   * @param <T> the type of the result
   */
  public static <T> Result<T> of(Supplier<T> supplier, Executor executor) {
    return of(CompletableFuture.supplyAsync(supplier), executor);
  }

  /**
   * See {@link CompletableFuture#supplyAsync(Supplier)}.
   *
   * @param supplier the supplier
   * @param <T> the type of the result
   */
  public static <T> Result<T> of(Supplier<T> supplier) {
    return of(CompletableFuture.supplyAsync(supplier));
  }

  /**
   * Creates and returns a new completed result.
   *
   * @param value the value
   * @param executor the executor
   * @param <T> the type of the result
   */
  public static <T> Result<T> of(T value, Executor executor) {
    return new Result<>(
        CompletableFuture.completedFuture(value), executor, new AtomicReference<>());
  }

  /**
   * Creates and returns a new completed result.
   *
   * @param value the value
   * @param <T> the type of the result
   */
  public static <T> Result<T> of(T value) {
    final var future = CompletableFuture.completedFuture(value);
    return new Result<>(future, future.defaultExecutor(), new AtomicReference<>());
  }

  /**
   * Creates and returns a new completed empty result.
   *
   * @param <T> the type of the result
   */
  public static <T> Result<T> empty() {
    final var future = CompletableFuture.<T>completedFuture(null);
    return new Result<>(future, future.defaultExecutor(), new AtomicReference<>());
  }

  /**
   * See {@link CompletableFuture#anyOf(CompletableFuture[])} and {@link
   * Result#of(CompletableFuture, Executor)}.
   *
   * @param results the results
   * @param executor the executor
   * @param <T> the type of the result
   */
  public static <T> Result<T> any(Stream<Result<T>> results, Executor executor) {
    return of(
        (CompletableFuture<T>)
            CompletableFuture.anyOf(results.map(Result::unbox).toArray(CompletableFuture[]::new)),
        executor);
  }

  /**
   * See {@link CompletableFuture#anyOf(CompletableFuture[])} and {@link
   * Result#of(CompletableFuture)}.
   *
   * @param results the results
   * @param <T> the type of the result
   */
  public static <T> Result<T> any(Stream<Result<T>> results) {
    return of(
        (CompletableFuture<T>)
            CompletableFuture.anyOf(results.map(Result::unbox).toArray(CompletableFuture[]::new)));
  }

  /**
   * Returns a new joined result in a stream from the given results.
   *
   * <p>See {@link Result#of(CompletableFuture, Executor)}.
   *
   * @param results the results
   * @param executor the executor
   * @param <T> the type of the result
   */
  public static <T> Result<Stream<T>> all(Collection<Result<T>> results, Executor executor) {
    final var builder = Stream.<T>builder();
    final var count = new AtomicInteger();
    final var size = results.size();
    final var future = new CompletableFuture<Stream<T>>();
    for (var result : results) {
      result
          .executor(executor)
          .accept(
              thing -> {
                builder.add(thing);
                if (count.incrementAndGet() == size) {
                  future.complete(builder.build());
                }
              });
    }
    return of(future, executor);
  }

  /**
   * Returns a new joined result in a stream from the given results.
   *
   * <p>See {@link Result#of(CompletableFuture)}.
   *
   * @param results the results
   * @param <T> the type of the result
   */
  public static <T> Result<Stream<T>> all(Collection<Result<T>> results) {
    final var builder = Stream.<T>builder();
    final var count = new AtomicInteger();
    final var size = results.size();
    final var future = new CompletableFuture<Stream<T>>();
    for (var result : results) {
      result.accept(
          thing -> {
            builder.add(thing);
            if (count.incrementAndGet() == size) {
              future.complete(builder.build());
            }
          });
    }
    return of(future);
  }

  /**
   * Sets the executor.
   *
   * @param executor the executor
   */
  public Result<T> executor(Executor executor) {
    if (this.executor == executor) {
      return this;
    }
    return new Result<>(completableFuture, executor, timeoutListeners);
  }

  /** Returns the executor. */
  public Executor executor() {
    return executor;
  }

  /**
   * Returns whether this is using the default executor.
   *
   * <p>See {@link CompletableFuture#defaultExecutor()}.
   */
  public boolean isUsingDefaultExecutor() {
    return executor == completableFuture.defaultExecutor();
  }

  /**
   * Sets the timeout duration.
   *
   * @param amount the amount
   * @param unit the unit
   */
  public Result<T> timeoutAfter(int amount, TimeUnit unit) {
    return new Result<>(
        completableFuture
            .orTimeout(amount, unit)
            .whenCompleteAsync(
                (t, throwable) -> {
                  if (throwable instanceof TimeoutException) {
                    notifyTimeoutListeners();
                  }
                }),
        executor,
        timeoutListeners);
  }

  /**
   * Sets the timeout duration with a default value.
   *
   * @param amount the amount
   * @param unit the unit
   * @param value the value
   */
  public Result<T> timeoutAfter(int amount, TimeUnit unit, Supplier<T> value) {
    return new Result<>(
            this.<Supplier<T>>map(thing -> () -> thing)
                .completableFuture
                .completeOnTimeout(
                    () -> {
                      notifyTimeoutListeners();
                      return value.get();
                    },
                    amount,
                    unit),
            executor,
            timeoutListeners)
        .map(Supplier::get);
  }

  /**
   * Adds a runnable that will be called if this result times out.
   *
   * @param runnable the runnable
   */
  public Result<T> onTimeout(Runnable runnable) {
    var runnables = timeoutListeners.get();
    if (runnables == null) {
      synchronized (timeoutListeners) {
        runnables = timeoutListeners.get();
        if (runnables == null) {
          runnables = Lists.newArrayList(runnable);
          timeoutListeners.set(runnables);
          return this;
        }
      }
    }
    runnables.add(runnable);
    return this;
  }

  /**
   * Returns a composed result.
   *
   * @param function the function
   * @param <R> the type of the new result
   */
  public <R> Result<R> compose(Function<? super T, CompletableFuture<R>> function) {
    return new Result<>(
        completableFuture.thenComposeAsync(function, executor), executor, timeoutListeners);
  }

  /**
   * Returns a new result by applying the given function to the current result.
   *
   * @param function the function
   * @param <R> the type of the new result
   */
  public <R> Result<R> map(Function<? super T, ? extends R> function) {
    return new Result<>(
        completableFuture.thenApplyAsync(function, executor), executor, timeoutListeners);
  }

  /**
   * Returns a composed result.
   *
   * @param function the function
   * @param <R> the type of the new result
   */
  public <R> Result<R> flatMap(Function<? super T, Result<R>> function) {
    return compose(thing -> function.apply(thing).completableFuture);
  }

  /**
   * Applies the given function with the given argument to the result.
   *
   * @param function the function
   * @param argument the argument
   * @param <U> the type of the argument
   * @param <R> the type of the new result
   */
  public <U, R> Result<R> apply(BiFunction<T, U, R> function, Supplier<U> argument) {
    return map(thing -> function.apply(thing, argument.get()));
  }

  /**
   * Returns a filtered result.
   *
   * @param predicate the predicate
   */
  public Result<T> filter(Predicate<? super T> predicate) {
    return map(
        thing -> {
          if (!predicate.test(thing)) {
            throw new PredicateFailException();
          }
          return thing;
        });
  }

  /** Returns a filtered not empty result. */
  public Result<T> filterNotEmpty() {
    return filter(Objects::nonNull);
  }

  /**
   * Accepts the given consumer when the result is completed.
   *
   * @param consumer the consumer
   */
  public Result<T> accept(Consumer<? super T> consumer) {
    return map(
        thing -> {
          consumer.accept(thing);
          return thing;
        });
  }

  /**
   * Accepts the given consumer with the given argument to the result.
   *
   * @param argument the argument
   * @param <U> the type of the argument
   */
  public <U> Result<T> accept(BiConsumer<T, U> consumer, Supplier<U> argument) {
    return accept(thing -> consumer.accept(thing, argument.get()));
  }

  /** Returns the inner completable future. */
  public CompletableFuture<T> unbox() {
    return completableFuture;
  }

  /** See {@link CompletableFuture#join()}. */
  public T join() {
    try {
      return completableFuture.join();
    } catch (CompletionException completionException) {
      if (completionException.getCause() instanceof PredicateFailException) {
        // ignore
        return null;
      }
      throw completionException;
    }
  }

  private void notifyTimeoutListeners() {
    final var runnables = timeoutListeners.get();
    if (runnables != null) {
      for (var runnable : runnables) {
        runnable.run();
      }
    }
  }

  private static class PredicateFailException extends RuntimeException {}
}
