package com.scofu.network.message.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.scofu.common.json.DynamicReference.dynamic;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

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
import com.scofu.network.message.ReplyingSubscription;
import com.scofu.network.message.Result;
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
  InternalMessageFlow(
      Json json,
      TypeCache typeCache,
      ObservableTopics observableTopics,
      PendingRequestStore pendingRequestStore) {
    this.json = json;
    this.typeCache = typeCache;
    this.observableTopics = observableTopics;
    this.pendingRequestStore = pendingRequestStore;
    this.subscriptions =
        Multimaps.newSetMultimap(Maps.newConcurrentMap(), Sets::newConcurrentHashSet);
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
    System.out.printf(
        "%sreceived: %s%s%n", "\u001B[36m", json.toString(Payload.class, payload), "\u001B[0m");
    if (payload == null) {
      throw new IllegalStateException(
          String.format("Couldn't parse payload: %s", new String(message, StandardCharsets.UTF_8)));
    }
    final var isReplyToRequest = payload.message() == null;
    if (isReplyToRequest) {
      completeRequest(payload);
      return completedFuture(null);
    }
    final var isMessageUnknown = payload.message().type() == null;
    if (isMessageUnknown) {
      return completedFuture(null);
    }
    return resolveReplyThroughSubscription(payload);
  }

  @Override
  public <T> ReplyingSubscription<T, Void> subscribeTo(TypeLiteral<T> type) {
    checkNotNull(type, "type");
    return new InternalReplyingSubscription<>(
        SubscriptionKey.of(type, null, new HashSet<>(), null), this::subscribe);
  }

  @Override
  public <T> ReplyingSubscription<T, Void> subscribeTo(Class<T> type) {
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

  private <T, R> void subscribe(SubscriptionKey<T, R> subscriptionKey) {
    final var key =
        typeCache.asString(subscriptionKey.type().getType())
            + (subscriptionKey.replyType() == null
                ? ""
                : typeCache.asString(subscriptionKey.replyType().getType()));
    subscriptions.put(key, subscriptionKey.function());
    if (subscriptionKey.topics().isEmpty()) {
      subscriptionKey.topics().add("global");
    }
    observableTopics.addAll(subscriptionKey.topics());
  }

  private void completeRequest(Payload payload) {
    pendingRequestStore
        .poll(payload.id())
        .ifPresent(future -> future.complete(payload.reply().value()));
  }

  private CompletableFuture<byte[]> resolveReplyThroughSubscription(Payload payload) {
    final var key =
        payload.message().type() + (payload.reply() == null ? "" : payload.reply().type());
    final var subscriptions = this.subscriptions.get(key);
    if (subscriptions == null || subscriptions.isEmpty()) {
      return completedFuture(null);
    }
    System.out.println("1key = " + key);
    System.out.println("1subscriptions = " + subscriptions.stream().toList());
    return supplyAsync(
        () -> {
          final var message = payload.message().value();
          for (var subscription : subscriptions) {
            final var reply =
                (byte[])
                    ((Result) subscription.apply(message))
                        .map(resolved -> serializePayload(payload, resolved))
                        .join();
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
    final var replyPayload =
        Payload.of(payload.id(), null, dynamic(payload.reply().type(), resolvedReply));
    System.out.printf(
        "%ssent reply: %s%s%n",
        "\u001B[34m", json.toString(Payload.class, replyPayload), "\u001B[0m");
    return json.toBytes(Payload.class, replyPayload);
  }
}
