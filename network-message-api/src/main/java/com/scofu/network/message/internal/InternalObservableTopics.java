package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.scofu.network.message.ObservableTopics;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

final class InternalObservableTopics implements ObservableTopics {

  private final Set<String> topics;
  private final Set<Consumer<? super String>> listeners;

  @Inject
  InternalObservableTopics() {
    this.topics = Sets.newConcurrentHashSet();
    this.listeners = Sets.newConcurrentHashSet();
  }

  @Override
  public void add(String topic) {
    checkNotNull(topic, "topic");
    addAndNotifyListenersIfNew(topic);
  }

  @Override
  public void addAll(Collection<? extends String> topics) {
    checkNotNull(topics, "topics");
    topics.forEach(this::addAndNotifyListenersIfNew);
  }

  @Override
  public void observe(Consumer<? super String> consumer) {
    checkNotNull(consumer, "consumer");
    topics.forEach(consumer);
    listeners.add(consumer);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("topics", topics)
        .add("listeners", listeners)
        .toString();
  }

  @Override
  public Iterator<String> iterator() {
    return topics.iterator();
  }

  private void addAndNotifyListenersIfNew(String topic) {
    final var isNewTopic = this.topics.add(topic);
    if (isNewTopic) {
      listeners.forEach(consumer -> consumer.accept(topic));
    }
  }
}
