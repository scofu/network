package com.scofu.network.message;

import com.google.inject.TypeLiteral;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the flow of subscriptions to messages and (internally) preparations for replies.
 *
 * <p>See {@link MessageQueue}.
 */
public interface MessageFlow {

  /**
   * Returns the topics.
   */
  ObservableTopics topics();

  /**
   * Handles the given message and returns an optional reply.
   *
   * @param message the message
   */
  CompletableFuture<byte[]> handleMessageOrRequest(byte... message);

  <T> ReplyingSubscriptionBuilder<T, Void> subscribeTo(TypeLiteral<T> type);

  <T> ReplyingSubscriptionBuilder<T, Void> subscribeTo(Class<T> type);
}
