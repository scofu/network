package com.scofu.network.message;

/**
 * Forwarded builder for {@link ReplyingSubscriptionBuilder} and {@link SubscriptionBuilder}.
 *
 * @param <S> the type of the parent builder
 */
public interface TopicSubscriptionBuilder<S> {

  /**
   * Adds the given topic.
   *
   * @param topic the topic
   */
  S withTopic(String topic);

  /**
   * Adds the given topics.
   *
   * @param firstTopic the first topic
   * @param moreTopics the rest of the topics
   */
  S withTopics(String firstTopic, String... moreTopics);
}
