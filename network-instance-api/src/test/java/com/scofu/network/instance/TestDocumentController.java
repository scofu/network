package com.scofu.network.instance;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.Json;
import com.scofu.network.document.api.DocumentCountReply;
import com.scofu.network.document.api.DocumentCountRequest;
import com.scofu.network.document.api.DocumentDeleteReply;
import com.scofu.network.document.api.DocumentDeleteRequest;
import com.scofu.network.document.api.DocumentDeletedMessage;
import com.scofu.network.document.api.DocumentQueryReply;
import com.scofu.network.document.api.DocumentQueryRequest;
import com.scofu.network.document.api.DocumentUpdateReply;
import com.scofu.network.document.api.DocumentUpdateRequest;
import com.scofu.network.document.api.DocumentUpdatedMessage;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class TestDocumentController implements Feature {

  private final MessageQueue messageQueue;
  private final Json json;
  private final Map<String, Collection> database;

  @Inject
  TestDocumentController(MessageQueue messageQueue, MessageFlow messageFlow, Json json) {
    this.messageQueue = messageQueue;
    this.database = Maps.newConcurrentMap();
    this.json = json;
    subscribeToRequests(messageFlow);
  }

  private void subscribeToRequests(MessageFlow messageFlow) {
    messageFlow
        .subscribeTo(DocumentQueryRequest.class)
        .replyWith(DocumentQueryReply.class)
        .withTopic("scofu.document.query.#")
        .via(this::onDocumentQueryRequest);
    messageFlow
        .subscribeTo(DocumentCountRequest.class)
        .replyWith(DocumentCountReply.class)
        .withTopic("scofu.document.count.#")
        .via(this::onDocumentCountRequest);
    messageFlow
        .subscribeTo(DocumentUpdateRequest.class)
        .replyWith(DocumentUpdateReply.class)
        .withTopic("scofu.document.update.#")
        .via(this::onDocumentUpdateRequest);
    messageFlow
        .subscribeTo(DocumentDeleteRequest.class)
        .replyWith(DocumentDeleteReply.class)
        .withTopic("scofu.document.delete.#")
        .via(this::onDocumentDeleteRequest);
  }

  private CompletableFuture<DocumentQueryReply> onDocumentQueryRequest(
      DocumentQueryRequest request) {
    try {
      final var collection = database.get(request.collection());
      if (collection == null) {
        return completedFuture(new DocumentQueryReply(true, null, Map.of()));
      }
      final var sorted = request.query().sort() != null;
      final var expectedSize = request.query().limit() > 0 ? request.query().limit() : 16;
      final Map<String, String> reply =
          sorted
              ? Maps.newLinkedHashMapWithExpectedSize(expectedSize)
              : Maps.newHashMapWithExpectedSize(expectedSize);
      reply.putAll(collection.documents);
      return completedFuture(new DocumentQueryReply(true, null, reply));
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return completedFuture(new DocumentQueryReply(false, "Error", Map.of()));
    }
  }

  private CompletableFuture<DocumentCountReply> onDocumentCountRequest(
      DocumentCountRequest request) {
    final var collection = database.get(request.collection());
    if (collection == null) {
      return completedFuture(new DocumentCountReply(true, null, 0));
    }
    return completedFuture(new DocumentCountReply(true, null, collection.documents().size()));
  }

  private CompletableFuture<DocumentUpdateReply> onDocumentUpdateRequest(
      DocumentUpdateRequest request) {
    var collection = database.get(request.collection());
    if (collection == null) {
      collection = new Collection(Maps.newConcurrentMap());
      database.put(request.collection(), collection);
    }
    final var document =
        (Map<String, Object>)
            json.fromString(new TypeToken<Map<String, Object>>() {}.getType(), request.json());
    collection.documents.put((String) document.get("_id"), request.json());
    messageQueue
        .declareFor(DocumentUpdatedMessage.class)
        .withTopic("scofu.document.updated." + request.collection())
        .push(new DocumentUpdatedMessage(request.collection(), request.json()));
    return completedFuture(new DocumentUpdateReply(true, null));
  }

  private CompletableFuture<DocumentDeleteReply> onDocumentDeleteRequest(
      DocumentDeleteRequest request) {
    final var collection = database.get(request.collection());
    if (collection == null) {
      return completedFuture(new DocumentDeleteReply(true, null));
    }
    collection.documents.remove(request.id());
    messageQueue
        .declareFor(DocumentDeletedMessage.class)
        .withTopic("scofu.document.deleted." + request.collection())
        .push(new DocumentDeletedMessage(request.collection(), request.id()));
    return completedFuture(new DocumentDeleteReply(true, null));
  }

  private static class Collection {

    private final Map<String, String> documents;

    private Collection(Map<String, String> documents) {
      this.documents = documents;
    }

    public Map<String, String> documents() {
      return documents;
    }
  }
}
