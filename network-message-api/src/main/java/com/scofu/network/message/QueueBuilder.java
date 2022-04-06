package com.scofu.network.message;

import java.util.concurrent.CompletableFuture;

/**
 * Builds queues.
 *
 * <p>See {@link ReplyingQueueBuilder}.
 *
 * @param <T> the type of the message
 * @param <R> the type of the expected reply ({@link Void} if no reply is expected)
 */
public interface QueueBuilder<T, R> {

  /**
   * Pushes the given message.
   *
   * @param message the message
   */
  CompletableFuture<R> push(T message);

  /**
   * Sets the topic.
   *
   * @param topic the topic
   */
  QueueBuilder<T, R> withTopic(String topic);
}
