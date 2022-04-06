package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.scofu.common.json.DynamicReference.dynamic;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.scofu.common.json.Json;
import com.scofu.common.json.TypeCache;
import com.scofu.network.message.Dispatcher;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.NetworkMessageModule;
import com.scofu.network.message.ReplyingQueueBuilder;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.inject.Named;

final class InternalMessageQueue implements MessageQueue {

  private final TypeCache typeCache;
  private final Json json;
  private final MessageFlow messageFlow;
  private final Dispatcher dispatcher;
  private final ExecutorService executorService;
  private final PendingRequestStore pendingRequestStore;

  @Inject
  InternalMessageQueue(TypeCache typeCache, Json json, MessageFlow messageFlow,
      Dispatcher dispatcher,
      @Named(NetworkMessageModule.PUBLISHER_EXECUTOR_NAME) ExecutorService executorService,
      PendingRequestStore pendingRequestStore) {
    this.typeCache = typeCache;
    this.json = json;
    this.messageFlow = messageFlow;
    this.dispatcher = dispatcher;
    this.executorService = executorService;
    this.pendingRequestStore = pendingRequestStore;
  }

  @Override
  public ExecutorService executorService() {
    return executorService;
  }

  @Override
  public <T> ReplyingQueueBuilder<T, Void> declareFor(TypeLiteral<T> type) {
    checkNotNull(type, "type");
    return new InternalQueueBuilder<>(Queue.of(type, Types.voidTypeLiteral(), "global"),
        this::declareFor);
  }

  @Override
  public <T> ReplyingQueueBuilder<T, Void> declareFor(Class<T> type) {
    checkNotNull(type, "type");
    return declareFor(TypeLiteral.get(type));
  }

  private <T, R> CompletableFuture<R> declareFor(T t, Queue<T, R> queue) {
    return CompletableFuture.supplyAsync(() -> {
      final var expectsReply = !void.class.isAssignableFrom(queue.replyType().getRawType());
      if (expectsReply) {
        return dispatchPayloadAsRequest(t, queue);
      }
      return dispatchPayloadAsFanout(t, queue);
    }, executorService).thenCompose(Function.identity());
  }

  @Override
  public void close() throws IOException {
    executorService.shutdownNow();
    dispatcher.close();
  }

  private <T, R> CompletableFuture<R> dispatchPayloadAsRequest(T t, Queue<T, R> queue) {
    final var future = new CompletableFuture<R>().orTimeout(60, TimeUnit.SECONDS);
    final var requestId = pendingRequestStore.prepare(future);
    final var payload = createPayload(t, queue, requestId);
    System.out.printf("%ssent: %s%s%n", "\u001B[35m", json.toString(Payload.class, payload),
        "\u001B[0m");
    final var body = json.toBytes(Payload.class, payload);
    dispatcher.dispatchRequest(queue.topic(), body);
    if (future.isDone()) {
      System.out.println("FUTURE WAS ALREADY COMPLETED LOL TOO FAST");
      return CompletableFuture.completedFuture(future.join());
    }
    return future;
  }

  private <T, R> CompletableFuture<R> dispatchPayloadAsFanout(T t, Queue<T, R> queue) {
    final var future = CompletableFuture.<R>completedFuture(null);
    final var payload = createPayload(t, queue, null);
    System.out.printf("%ssent: %s%s%n", "\u001B[35m", json.toString(Payload.class, payload),
        "\u001B[0m");
    final var body = json.toBytes(Payload.class, payload);
    dispatcher.dispatchFanout(queue.topic(), body);
    return future;
  }

  private <T, R> Payload createPayload(T t, Queue<T, R> queue, String requestId) {
    final var dynamicMessage = dynamic(typeCache, queue.type(), t);
    final var dynamicReply = requestId == null ? null : dynamic(typeCache, queue.replyType(), null);
    return Payload.of(requestId, dynamicMessage, dynamicReply);
  }
}
