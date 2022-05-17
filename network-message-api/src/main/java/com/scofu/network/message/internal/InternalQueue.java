package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.TypeLiteral;
import com.scofu.network.message.Queue;
import com.scofu.network.message.ReplyingQueue;
import com.scofu.network.message.Result;
import java.util.function.BiFunction;

final class InternalQueue<T, R> implements ReplyingQueue<T, R> {

  private final QueueKey<T, R> key;
  private final BiFunction<T, QueueKey<T, ?>, Result<?>> function;

  public InternalQueue(QueueKey<T, R> key, BiFunction<T, QueueKey<T, ?>, Result<?>> function) {
    this.key = key;
    this.function = function;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result<R> push(T message) {
    checkNotNull(message, "message");
    return (Result<R>) function.apply(message, key);
  }

  @Override
  public <V> Queue<T, V> expectReply(TypeLiteral<V> replyType) {
    checkNotNull(replyType, "replyType");
    return new InternalQueue<>(QueueKey.of(key.type(), replyType, key.topic()), function);
  }

  @Override
  public <V> Queue<T, V> expectReply(Class<V> replyType) {
    checkNotNull(replyType, "replyType");
    return expectReply(TypeLiteral.get(replyType));
  }

  @Override
  public Queue<T, R> withTopic(String topic) {
    checkNotNull(topic, "topic");
    return new InternalQueue<>(QueueKey.of(key.type(), key.replyType(), topic), function);
  }
}
