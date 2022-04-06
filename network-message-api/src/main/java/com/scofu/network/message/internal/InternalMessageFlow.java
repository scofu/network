package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.scofu.common.json.DynamicReference.dynamic;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.scofu.common.json.Json;
import com.scofu.common.json.TypeCache;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.ObservableTopics;
import com.scofu.network.message.ReplyingSubscriptionBuilder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
final class InternalMessageFlow implements MessageFlow {

  private final Json json;
  private final TypeCache typeCache;
  private final ObservableTopics observableTopics;
  private final PendingRequestStore pendingRequestStore;
  private final Multimap<String, Function> subscriptions;

  @Inject
  InternalMessageFlow(Json json, TypeCache typeCache, ObservableTopics observableTopics,
      PendingRequestStore pendingRequestStore) {
    this.json = json;
    this.typeCache = typeCache;
    this.observableTopics = observableTopics;
    this.pendingRequestStore = pendingRequestStore;
    this.subscriptions = Multimaps.newSetMultimap(Maps.newConcurrentMap(),
        Sets::newConcurrentHashSet);
  }

  @Override
  public ObservableTopics topics() {
    return observableTopics;
  }

  @Override
  public CompletableFuture<byte[]> handleMessageOrRequest(byte... message) {
    System.out.println("received bytes: " + message.length);
    checkNotNull(message, "message");
    final var payload = json.fromBytes(Payload.class, message);
    System.out.printf("%sreceived: %s%s%n", "\u001B[36m", json.toString(Payload.class, payload),
        "\u001B[0m");
    if (payload == null) {
      throw new IllegalStateException(
          String.format("Couldn't parse payload: %s", new String(message, StandardCharsets.UTF_8)));
    }
    final var isReplyToRequest = payload.message() == null;
    if (isReplyToRequest) {
      completeRequest(payload);
      return CompletableFuture.completedFuture(null);
    }
    final var isMessageUnknown = payload.message().type() == null;
    if (isMessageUnknown) {
      return CompletableFuture.completedFuture(null);
    }
    return resolveReplyThroughSubscription(payload);
  }

  @Override
  public <T> ReplyingSubscriptionBuilder<T, Void> subscribeTo(TypeLiteral<T> type) {
    checkNotNull(type, "type");
    return new InternalReplyingSubscriptionBuilder<>(
        Subscription.of(type, null, new HashSet<>(), null), this::subscribe);
  }

  @Override
  public <T> ReplyingSubscriptionBuilder<T, Void> subscribeTo(Class<T> type) {
    checkNotNull(type, "type");
    return subscribeTo(TypeLiteral.get(type));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("typeCache", typeCache)
        .add("observableTopics", observableTopics)
        .add("pendingRequestStore", pendingRequestStore)
        .add("subscriptions", subscriptions)
        .toString();
  }

  private <T, R> void subscribe(Subscription<T, R> subscription) {
    final var key =
        typeCache.asString(subscription.type().getType()) + (subscription.replyType() == null ? ""
            : typeCache.asString(subscription.replyType().getType()));
    subscriptions.put(key, subscription.function());
    if (subscription.topics().isEmpty()) {
      subscription.topics().add("global");
    }
    observableTopics.addAll(subscription.topics());
  }

  private void completeRequest(Payload payload) {
    pendingRequestStore.poll(payload.id())
        .ifPresent(future -> future.complete(payload.reply().value()));
  }

  private CompletableFuture<byte[]> resolveReplyThroughSubscription(Payload payload) {
    final var key =
        payload.message().type() + (payload.reply() == null ? "" : payload.reply().type());
    final var subscriptions = this.subscriptions.get(key);
    final var hasSubscription = subscriptions != null && !subscriptions.isEmpty();
    if (!hasSubscription) {
      return CompletableFuture.completedFuture(null);
    }
    System.out.println("1key = " + key);
    System.out.println("1subscriptions = " + subscriptions.stream().toList());

    return CompletableFuture.supplyAsync(() -> {
      final var message = payload.message().value();
      for (var subscription : subscriptions) {
        final var reply = (byte[]) ((CompletableFuture) subscription.apply(message)).thenApply(
            resolved -> serializePayload(payload, resolved)).join();
        if (reply != null) {
          return reply;
        }
      }
      return null;
    });
  }

  private byte[] serializePayload(Payload payload, Object resolvedReply) {
    if (payload.id() == null || resolvedReply == null) {
      return null;
    }
    final var replyPayload = Payload.of(payload.id(), null,
        dynamic(payload.reply().type(), resolvedReply));
    System.out.printf("%ssent reply: %s%s%n", "\u001B[34m",
        json.toString(Payload.class, replyPayload), "\u001B[0m");
    return json.toBytes(Payload.class, replyPayload);
  }
}
