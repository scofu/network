package com.scofu.network.message;

import com.google.inject.TypeLiteral;

/**
 * Builds queues that can chose to expect a reply.
 *
 * @param <T> the type of the message
 * @param <R> the type of the expected reply ({@link Void} if no reply is expected)
 */
public interface ReplyingQueue<T, R> extends Queue<T, R> {

  /**
   * Returns a queue builder with the given expected reply.
   *
   * @param replyType the type of the reply
   * @param <V> the type of the reply
   */
  <V> Queue<T, V> expectReply(TypeLiteral<V> replyType);

  /**
   * Returns a queue builder with the given expected reply.
   *
   * @param replyType the type of the reply
   * @param <V> the type of the reply
   */
  <V> Queue<T, V> expectReply(Class<V> replyType);
}
