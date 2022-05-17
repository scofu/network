package com.scofu.network.message;

import com.google.inject.TypeLiteral;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;

/**
 * Builds "queues" for messages and requests. <br>
 * This does not represent an actual AMQP queue. Messages "pushed" here are internally (under AMQP)
 * sent in a global exchange with the topic as its routing key. <br>
 * A queue holds the context of the flow of a single message or request. Internally it is simply
 * represented as two type literals and a topic, the type of the message and the type of the
 * expected reply. If the type of the reply is {@link Void}, no reply is expected. <br>
 * Messages that expect a reply, when pushed, prepares for such reply by registering a {@link
 * java.util.concurrent.CompletableFuture} in the message flow. Which is kept in memory until the
 * request is complete, expiring after a set duration.
 */
public interface MessageQueue extends Closeable {

  /** Returns the executor service. */
  ExecutorService executorService();

  /**
   * Sets the type of the message.
   *
   * @param type the type of the message
   * @param <T> the type of the message
   */
  <T> ReplyingQueue<T, Void> declareFor(TypeLiteral<T> type);

  /**
   * Sets the type of the message.
   *
   * @param type the type of the message
   * @param <T> the type of the message
   */
  <T> ReplyingQueue<T, Void> declareFor(Class<T> type);
}
