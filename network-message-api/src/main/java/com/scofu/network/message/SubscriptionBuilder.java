package com.scofu.network.message;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Builds subscriptions.
 *
 * <p>See {@link ReplyingSubscriptionBuilder}.
 *
 * @param <T> the type of the message
 * @param <R> the type of the reply ({@link Void} if subscription isn't replying)
 */
public interface SubscriptionBuilder<T, R>
    extends TopicSubscriptionBuilder<SubscriptionBuilder<T, R>> {

  /**
   * Sets the function for the subscription that receives the message and returns a reply.
   *
   * @param function the function
   */
  void via(Function<T, CompletableFuture<? extends R>> function);
}
