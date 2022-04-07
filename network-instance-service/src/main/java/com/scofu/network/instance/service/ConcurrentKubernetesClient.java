package com.scofu.network.instance.service;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class ConcurrentKubernetesClient {

  private final KubernetesClient client;
  private final ExecutorService executorService;

  public ConcurrentKubernetesClient(KubernetesClient client, ExecutorService executorService) {
    this.client = client;
    this.executorService = executorService;
  }

  public static ConcurrentKubernetesClient of() {
    final var executorService = Executors.newSingleThreadExecutor();
    var future = new CompletableFuture<ConcurrentKubernetesClient>();
    executorService.execute(() -> {
      try (var client = new DefaultKubernetesClient()) {
        future.complete(new ConcurrentKubernetesClient(client, executorService));
      }
    });
    return future.join();
  }

  public void accept(ThrowingConsumer<? super KubernetesClient> consumer) {
    executorService.execute(() -> consumer.accept(client));
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