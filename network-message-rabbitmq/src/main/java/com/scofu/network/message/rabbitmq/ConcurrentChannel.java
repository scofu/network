package com.scofu.network.message.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

final class ConcurrentChannel {

  private final Connection connection;
  private final Channel channel;
  private final ExecutorService executorService;

  ConcurrentChannel(Connection connection, ExecutorService executorService) throws IOException {
    this.connection = connection;
    this.channel = connection.createChannel();
    this.executorService = executorService;
  }

  static ConcurrentChannel of(ConnectionFactory factory) {
    final var executorService = Executors.newSingleThreadExecutor();
    var future = new CompletableFuture<ConcurrentChannel>();
    executorService.execute(
        () -> {
          try {
            future.complete(new ConcurrentChannel(factory.newConnection(), executorService));
          } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
          }
        });
    return future.join();
  }

  public void accept(ThrowingConsumer<? super Channel> consumer) {
    executorService.execute(
        () -> {
          try {
            consumer.accept(channel);
          } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.out.println("ERROR RMQ");
          }
        });
  }

  public Connection connection() {
    return connection;
  }

  public ExecutorService executorService() {
    return executorService;
  }

  interface ThrowingConsumer<T> extends Consumer<T> {

    void acceptThrowable(T t) throws Throwable;

    @Override
    default void accept(T t) {
      try {
        acceptThrowable(t);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
}
