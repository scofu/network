package com.scofu.network.message;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A subscription.
 *
 * <p>See {@link ReplyingSubscription}.
 *
 * @param <T> the type of the message
 * @param <R> the type of the reply ({@link Void} if subscription isn't replying)
 */
public interface Subscription<T, R> extends TopicSubscription<Subscription<T, R>> {

  /**
   * Sets the function for the subscription that receives the message and returns a reply.
   *
   * @param function the function
   */
  void via(Function<T, Result<? extends R>> function);
}
