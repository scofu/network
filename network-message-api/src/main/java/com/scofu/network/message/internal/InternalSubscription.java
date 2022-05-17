package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scofu.network.message.Result;
import com.scofu.network.message.Subscription;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class InternalSubscription<T, R> implements Subscription<T, R> {

  private final SubscriptionKey<T, R> key;
  private final Consumer<SubscriptionKey<?, ?>> consumer;

  public InternalSubscription(SubscriptionKey<T, R> key, Consumer<SubscriptionKey<?, ?>> consumer) {
    this.key = key;
    this.consumer = consumer;
  }

  @Override
  public void via(Function<T, Result<? extends R>> function) {
    checkNotNull(function, "function");
    consumer.accept(SubscriptionKey.of(key.type(), key.replyType(), key.topics(), function));
  }

  @Override
  public Subscription<T, R> withTopic(String topic) {
    checkNotNull(topic, "topic");
    key.topics().add(topic);
    return this;
  }

  @Override
  public Subscription<T, R> withTopics(String firstTopic, String... moreTopics) {
    checkNotNull(firstTopic, "firstTopic");
    checkNotNull(moreTopics, "moreTopics");
    key.topics().add(firstTopic);
    key.topics().addAll(Arrays.asList(moreTopics));
    return this;
  }
}
