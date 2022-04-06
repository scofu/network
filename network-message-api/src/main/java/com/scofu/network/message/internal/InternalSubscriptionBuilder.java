package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scofu.network.message.SubscriptionBuilder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class InternalSubscriptionBuilder<T, R> implements SubscriptionBuilder<T, R> {

  private final Subscription<T, R> subscription;
  private final Consumer<Subscription<?, ?>> consumer;

  public InternalSubscriptionBuilder(Subscription<T, R> subscription,
      Consumer<Subscription<?, ?>> consumer) {
    this.subscription = subscription;
    this.consumer = consumer;
  }

  @Override
  public void via(Function<T, CompletableFuture<? extends R>> function) {
    checkNotNull(function, "function");
    consumer.accept(
        Subscription.of(subscription.type(), subscription.replyType(), subscription.topics(),
            function));
  }

  @Override
  public SubscriptionBuilder<T, R> withTopic(String topic) {
    checkNotNull(topic, "topic");
    subscription.topics().add(topic);
    return this;
  }

  @Override
  public SubscriptionBuilder<T, R> withTopics(String firstTopic, String... moreTopics) {
    checkNotNull(firstTopic, "firstTopic");
    checkNotNull(moreTopics, "moreTopics");
    subscription.topics().add(firstTopic);
    subscription.topics().addAll(Arrays.asList(moreTopics));
    return this;
  }
}
