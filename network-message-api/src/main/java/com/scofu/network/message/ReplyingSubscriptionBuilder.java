package com.scofu.network.message;

import com.google.inject.TypeLiteral;
import java.util.function.Consumer;

/**
 * Builds subscriptions that can chose to also reply.
 *
 * @param <T> the type of the message
 * @param <R> the type of the reply ({@link Void} if subscription isn't replying)
 */
public interface ReplyingSubscriptionBuilder<T, R>
    extends TopicSubscriptionBuilder<ReplyingSubscriptionBuilder<T, R>> {

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
  <V> SubscriptionBuilder<T, V> replyWith(TypeLiteral<V> replyType);

  /**
   * Sets the type of the reply for the subscription.
   *
   * @param replyType the type of the reply.
   */
  <V> SubscriptionBuilder<T, V> replyWith(Class<V> replyType);
}
