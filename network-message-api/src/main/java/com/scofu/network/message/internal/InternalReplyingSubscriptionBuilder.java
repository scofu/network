package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.TypeLiteral;
import com.scofu.network.message.ReplyingSubscriptionBuilder;
import com.scofu.network.message.SubscriptionBuilder;
import java.util.Arrays;
import java.util.function.Consumer;

final class InternalReplyingSubscriptionBuilder<T, R> implements ReplyingSubscriptionBuilder<T, R> {

  private final Subscription<T, R> subscription;
  private final Consumer<Subscription<?, ?>> consumer;

  public InternalReplyingSubscriptionBuilder(
      Subscription<T, R> subscription, Consumer<Subscription<?, ?>> consumer) {
    this.subscription = subscription;
    this.consumer = consumer;
  }

  @Override
  public void via(Consumer<T> consumer) {
    checkNotNull(consumer, "consumer");
    this.consumer.accept(
        Subscription.of(
            subscription.type(),
            subscription.replyType(),
            subscription.topics(),
            ConsumerFunction.wrap(consumer)));
  }

  @Override
  public <V> SubscriptionBuilder<T, V> replyWith(TypeLiteral<V> replyType) {
    checkNotNull(replyType, "replyType");
    return new InternalSubscriptionBuilder<>(
        Subscription.of(subscription.type(), replyType, subscription.topics(), null), consumer);
  }

  @Override
  public <V> SubscriptionBuilder<T, V> replyWith(Class<V> replyType) {
    checkNotNull(replyType, "replyType");
    return replyWith(TypeLiteral.get(replyType));
  }

  @Override
  public ReplyingSubscriptionBuilder<T, R> withTopic(String topic) {
    checkNotNull(topic, "topic");
    subscription.topics().add(topic);
    return this;
  }

  @Override
  public ReplyingSubscriptionBuilder<T, R> withTopics(String firstTopic, String... moreTopics) {
    checkNotNull(firstTopic, "firstTopic");
    checkNotNull(moreTopics, "moreTopics");
    subscription.topics().add(firstTopic);
    subscription.topics().addAll(Arrays.asList(moreTopics));
    return this;
  }
}
