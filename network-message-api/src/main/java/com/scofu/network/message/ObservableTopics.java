package com.scofu.network.message;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Represents a dynamic set of topics, which can be observed.
 *
 * <p>Subscribed topics are only automatically added when registered to the flow. Declaring a queue
 * or pushing to a queue with a certain topic will not add it automatically.
 */
public interface ObservableTopics extends Iterable<String> {

  /**
   * Adds the topic.
   *
   * @param topic the topic
   */
  void add(String topic);

  /**
   * Adds all the topics.
   *
   * @param topics the topics
   */
  void addAll(Collection<? extends String> topics);

  /**
   * Registers the observer.
   *
   * @param consumer the consumer
   */
  void observe(Consumer<? super String> consumer);
}
