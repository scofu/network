package com.scofu.network.message;

import java.io.Closeable;

/**
 * Dispatches messages.
 */
public interface Dispatcher extends Closeable {

  /**
   * Dispatches the message, not expecting any reply.
   *
   * @param topic   the topic
   * @param message the message
   */
  void dispatchFanout(String topic, byte... message);

  /**
   * Dispatches the message, expecting a reply.
   *
   * @param topic   the topic
   * @param message the message
   */
  void dispatchRequest(String topic, byte... message);

}
