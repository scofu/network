package com.scofu.network.message;

import com.google.inject.TypeLiteral;

/**
 * Builds queues that can chose to expect a reply.
 *
 * @param <T> the type of the message
 * @param <R> the type of the expected reply ({@link Void} if no reply is expected)
 */
public interface ReplyingQueueBuilder<T, R> extends QueueBuilder<T, R> {

  /**
   * Returns a queue builder with the given expected reply.
   *
   * @param replyType the type of the reply
   * @param <V>       the type of the reply
   */
  <V> QueueBuilder<T, V> expectReply(TypeLiteral<V> replyType);

  /**
   * Returns a queue builder with the given expected reply.
   *
   * @param replyType the type of the reply
   * @param <V>       the type of the reply
   */
  <V> QueueBuilder<T, V> expectReply(Class<V> replyType);
}
