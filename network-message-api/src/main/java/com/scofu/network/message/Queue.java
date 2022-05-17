package com.scofu.network.message;

import java.util.concurrent.CompletableFuture;

/**
 * A queue.
 *
 * <p>See {@link ReplyingQueue}.
 *
 * @param <T> the type of the message
 * @param <R> the type of the expected reply ({@link Void} if no reply is expected)
 */
public interface Queue<T, R> {

  /**
   * Pushes the given message.
   *
   * @param message the message
   */
  Result<R> push(T message);

  /**
   * Sets the topic.
   *
   * @param topic the topic
   */
  Queue<T, R> withTopic(String topic);
}
