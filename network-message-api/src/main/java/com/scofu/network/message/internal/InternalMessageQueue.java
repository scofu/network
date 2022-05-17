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
import com.scofu.network.message.ReplyingQueue;
import com.scofu.network.message.Result;
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
  InternalMessageQueue(
      TypeCache typeCache,
      Json json,
      MessageFlow messageFlow,
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
  public <T> ReplyingQueue<T, Void> declareFor(TypeLiteral<T> type) {
    checkNotNull(type, "type");
    return new InternalQueue<>(
        QueueKey.of(type, Types.voidTypeLiteral(), "global"), this::declareFor);
  }

  @Override
  public <T> ReplyingQueue<T, Void> declareFor(Class<T> type) {
    checkNotNull(type, "type");
    return declareFor(TypeLiteral.get(type));
  }

  private <T, R> Result<R> declareFor(T t, QueueKey<T, R> queueKey) {
    return Result.of(
            () -> {
              final var expectsReply =
                  !void.class.isAssignableFrom(queueKey.replyType().getRawType());
              if (expectsReply) {
                return dispatchPayloadAsRequest(t, queueKey);
              }
              return dispatchPayloadAsFanout(t, queueKey);
            },
            executorService)
        .flatMap(Function.identity());
  }

  @Override
  public void close() throws IOException {
    executorService.shutdownNow();
    dispatcher.close();
  }

  private <T, R> Result<R> dispatchPayloadAsRequest(T t, QueueKey<T, R> queueKey) {
    final var future = new CompletableFuture<R>();
    final var requestId = pendingRequestStore.prepare(future);
    final var payload = createPayload(t, queueKey, requestId);
    System.out.printf(
        "%ssent: %s%s%n", "\u001B[35m", json.toString(Payload.class, payload), "\u001B[0m");
    final var body = json.toBytes(Payload.class, payload);
    dispatcher.dispatchRequest(queueKey.topic(), body);
    return Result.of(future)
        .timeoutAfter(1, TimeUnit.MINUTES)
        .onTimeout(() -> pendingRequestStore.invalidate(requestId));
  }

  private <T, R> Result<R> dispatchPayloadAsFanout(T t, QueueKey<T, R> queueKey) {
    final var future = CompletableFuture.<R>completedFuture(null);
    final var payload = createPayload(t, queueKey, null);
    System.out.printf(
        "%ssent: %s%s%n", "\u001B[35m", json.toString(Payload.class, payload), "\u001B[0m");
    final var body = json.toBytes(Payload.class, payload);
    dispatcher.dispatchFanout(queueKey.topic(), body);
    return Result.of(future);
  }

  private <T, R> Payload createPayload(T t, QueueKey<T, R> queueKey, String requestId) {
    final var dynamicMessage = dynamic(typeCache, queueKey.type(), t);
    final var dynamicReply =
        requestId == null ? null : dynamic(typeCache, queueKey.replyType(), null);
    return Payload.of(requestId, dynamicMessage, dynamicReply);
  }
}
