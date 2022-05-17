package com.scofu.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Tests timeout. */
public class TimeoutTest extends Service {

  @Inject private MessageQueue messageQueue;
  @Inject private MessageFlow messageFlow;

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bind(Dispatcher.class).to(LocalDispatcher.class).in(Scopes.SINGLETON);
  }

  @Test
  public void test() {
    if (true) {
      final var pool = Executors.newCachedThreadPool();
      final var rootFuture = new CompletableFuture<>();

      CompletableFuture.runAsync(
          () -> {
            try {
              TimeUnit.SECONDS.sleep(4);
              System.out.println("rootFuture = " + rootFuture);
              System.out.println("rootFuture.iCE() = " + rootFuture.isCompletedExceptionally());
              System.out.println("rootFuture.isDone() = " + rootFuture.isDone());
              System.out.println("rootFuture.isCancelled() = " + rootFuture.isCancelled());
              rootFuture.complete(1337);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          },
          pool);

      System.out.println("joined");

      final var result =
          Result.of(rootFuture, pool)
              .timeoutAfter(
                  3,
                  TimeUnit.SECONDS,
                  () -> {
                    System.out.println("running supplier!");
                    return 13377;
                  })
              .map(Object::toString)
              .map(s -> "got: " + s)
              .onTimeout(() -> System.out.println("Timed out..."))
              .accept(s -> System.out.println("ayo: " + s))
              .filter(s -> s.equals("got: 13377"))
              .flatMap(
                  s -> {
                    System.out.println("flat mapping!");
                    return Result.of(CompletableFuture.supplyAsync(() -> 420, pool), pool)
                        .map(i -> s + " " + i);
                  })
              .accept(s -> System.out.println("yo: " + s));

      final var result2 = Result.of(rootFuture, pool).map(Object::toString).map(s -> s + "lol");

      final var result3 = Result.of(CompletableFuture.completedFuture("hi"), pool);

      Result.all(List.of(result, result2, result3), pool)
          .apply(Stream::map, () -> (Function<String, String>) String::toUpperCase)
          .apply(Stream::collect, () -> Collectors.joining(", "))
          .timeoutAfter(10, TimeUnit.SECONDS, () -> "timed out")
          .onTimeout(() -> System.out.println("actually timed out :o"))
          .map(string -> "result: " + string)
          .accept(System.out::println);

      try {
        TimeUnit.SECONDS.sleep(15);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return;
    }

    load(Stage.PRODUCTION, this);
    messageFlow
        .subscribeTo(Ping.class)
        .replyWith(Ping.class)
        .via(
            ping ->
                Result.of(
                    () -> {
                      try {
                        TimeUnit.MINUTES.sleep(1);
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                      return ping;
                    }));
    final var pong =
        messageQueue
            .declareFor(Ping.class)
            .expectReply(Ping.class)
            .push(new Ping(System.nanoTime(), "hi"))
            .timeoutAfter(1, TimeUnit.SECONDS, () -> new Ping(0, "timed out"))
            .join();
    assertNotNull(pong);
    assertEquals("timed out", pong.tag());
  }
}
