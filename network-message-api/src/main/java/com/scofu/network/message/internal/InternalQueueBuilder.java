package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.TypeLiteral;
import com.scofu.network.message.QueueBuilder;
import com.scofu.network.message.ReplyingQueueBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

final class InternalQueueBuilder<T, R> implements ReplyingQueueBuilder<T, R> {

  private final Queue<T, R> queue;
  private final BiFunction<T, Queue<T, ?>, CompletableFuture<?>> function;

  public InternalQueueBuilder(Queue<T, R> queue,
      BiFunction<T, Queue<T, ?>, CompletableFuture<?>> function) {
    this.queue = queue;
    this.function = function;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CompletableFuture<R> push(T message) {
    checkNotNull(message, "message");
    return (CompletableFuture<R>) function.apply(message, queue);
  }

  @Override
  public <V> QueueBuilder<T, V> expectReply(TypeLiteral<V> replyType) {
    checkNotNull(replyType, "replyType");
    return new InternalQueueBuilder<>(Queue.of(queue.type(), replyType, queue.topic()), function);
  }

  @Override
  public <V> QueueBuilder<T, V> expectReply(Class<V> replyType) {
    checkNotNull(replyType, "replyType");
    return expectReply(TypeLiteral.get(replyType));
  }

  @Override
  public QueueBuilder<T, R> withTopic(String topic) {
    checkNotNull(topic, "topic");
    return new InternalQueueBuilder<>(Queue.of(queue.type(), queue.replyType(), topic), function);
  }
}
