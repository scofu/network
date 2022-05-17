package com.scofu.network.message;

import com.google.inject.TypeLiteral;
import java.util.function.Consumer;

/**
 * Subscription that can choose to also reply.
 *
 * @param <T> the type of the message
 * @param <R> the type of the reply ({@link Void} if subscription isn't replying)
 */
public interface ReplyingSubscription<T, R> extends TopicSubscription<ReplyingSubscription<T, R>> {

  /**
   * Sets the consumer for the subscription that receives the message.
   *
   * @param consumer the consumer
   */
  void via(Consumer<T> consumer);

  /**
   * Sets the type of the reply for the subscription.
   *
   * @param replyType the type of the reply.
   */
  <V> Subscription<T, V> replyWith(TypeLiteral<V> replyType);

  /**
   * Sets the type of the reply for the subscription.
   *
   * @param replyType the type of the reply.
   */
  <V> Subscription<T, V> replyWith(Class<V> replyType);
}
