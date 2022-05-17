package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.TypeLiteral;
import com.scofu.network.message.ReplyingSubscription;
import com.scofu.network.message.Subscription;
import java.util.Arrays;
import java.util.function.Consumer;

final class InternalReplyingSubscription<T, R> implements ReplyingSubscription<T, R> {

  private final SubscriptionKey<T, R> key;
  private final Consumer<SubscriptionKey<?, ?>> consumer;

  public InternalReplyingSubscription(
      SubscriptionKey<T, R> key, Consumer<SubscriptionKey<?, ?>> consumer) {
    this.key = key;
    this.consumer = consumer;
  }

  @Override
  public void via(Consumer<T> consumer) {
    checkNotNull(consumer, "consumer");
    this.consumer.accept(
        SubscriptionKey.of(
            key.type(), key.replyType(), key.topics(), ConsumerFunction.wrap(consumer)));
  }

  @Override
  public <V> Subscription<T, V> replyWith(TypeLiteral<V> replyType) {
    checkNotNull(replyType, "replyType");
    return new InternalSubscription<>(
        SubscriptionKey.of(key.type(), replyType, key.topics(), null), consumer);
  }

  @Override
  public <V> Subscription<T, V> replyWith(Class<V> replyType) {
    checkNotNull(replyType, "replyType");
    return replyWith(TypeLiteral.get(replyType));
  }

  @Override
  public ReplyingSubscription<T, R> withTopic(String topic) {
    checkNotNull(topic, "topic");
    key.topics().add(topic);
    return this;
  }

  @Override
  public ReplyingSubscription<T, R> withTopics(String firstTopic, String... moreTopics) {
    checkNotNull(firstTopic, "firstTopic");
    checkNotNull(moreTopics, "moreTopics");
    key.topics().add(firstTopic);
    key.topics().addAll(Arrays.asList(moreTopics));
    return this;
  }
}
