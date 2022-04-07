package com.scofu.network.document.service;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
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
import java.util.function.Consumer;
import org.bson.Document;
import org.bson.conversions.Bson;

final class DocumentController implements Feature {

  private final MessageQueue messageQueue;
  private final MongoDatabase mongoDatabase;
  private final Json json;

  @Inject
  DocumentController(MessageQueue messageQueue, MessageFlow messageFlow,
      MongoDatabase mongoDatabase, Json json) {
    this.messageQueue = messageQueue;
    this.mongoDatabase = mongoDatabase;
    this.json = json;
    subscribeToRequests(messageFlow);
  }

  private void subscribeToRequests(MessageFlow messageFlow) {
    messageFlow.subscribeTo(DocumentQueryRequest.class)
        .replyWith(DocumentQueryReply.class)
        .withTopic("scofu.document.query.#")
        .via(this::onDocumentQueryRequest);
    messageFlow.subscribeTo(DocumentCountRequest.class)
        .replyWith(DocumentCountReply.class)
        .withTopic("scofu.document.count.#")
        .via(this::onDocumentCountRequest);
    messageFlow.subscribeTo(DocumentUpdateRequest.class)
        .replyWith(DocumentUpdateReply.class)
        .withTopic("scofu.document.update.#")
        .via(this::onDocumentUpdateRequest);
    messageFlow.subscribeTo(DocumentDeleteRequest.class)
        .replyWith(DocumentDeleteReply.class)
        .withTopic("scofu.document.delete.#")
        .via(this::onDocumentDeleteRequest);
  }

  private CompletableFuture<DocumentQueryReply> onDocumentQueryRequest(
      DocumentQueryRequest request) {
    try {
      final var collection = mongoDatabase.getCollection(request.collection());
      final var sorted = request.query().sort() != null;
      final var expectedSize = request.query().limit() > 0 ? request.query().limit() : 16;
      final Map<String, String> reply = sorted ? Maps.newLinkedHashMapWithExpectedSize(expectedSize)
          : Maps.newHashMapWithExpectedSize(expectedSize);
      final var filter = request.query().filter() == null ? new Document()
          : new Document(request.query().filter().asMap());
      var matches = collection.find(filter);
      if (request.query().sort() != null) {
        matches = matches.sort(new Document(request.query().sort().asMap()));
      }
      if (request.query().skip() > 0) {
        matches = matches.skip(request.query().skip());
      }
      if (request.query().limit() > 0) {
        matches = matches.limit(request.query().limit());
      }
      matches.forEach((Consumer<? super Document>) document -> reply.put(document.getString("_id"),
          json.toString(Bson.class, document)));
      return CompletableFuture.completedFuture(new DocumentQueryReply(true, null, reply));
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      System.out.println("EROR");
      System.out.println(throwable);
      System.out.println(throwable.getCause());
      return CompletableFuture.completedFuture(new DocumentQueryReply(false, "Error", Map.of()));
    }
  }

  private CompletableFuture<DocumentCountReply> onDocumentCountRequest(
      DocumentCountRequest request) {
    final var collection = mongoDatabase.getCollection(request.collection());
    final var count = request.query().filter() == null ? collection.countDocuments()
        : collection.countDocuments(new Document(request.query().filter().asMap()));
    return CompletableFuture.completedFuture(new DocumentCountReply(true, null, count));
  }

  private CompletableFuture<DocumentUpdateReply> onDocumentUpdateRequest(
      DocumentUpdateRequest request) {
    final var collection = mongoDatabase.getCollection(request.collection());
    final var document = (Document) json.fromString(Bson.class, request.json());
    final UpdateResult result;
    try {
      result = collection.replaceOne(Filters.eq("_id", document.getString("_id")), document,
          new ReplaceOptions().upsert(true));
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return CompletableFuture.completedFuture(new DocumentUpdateReply(false, "Error"));
    }
    if (!result.wasAcknowledged()) {
      return CompletableFuture.completedFuture(
          new DocumentUpdateReply(false, "Database did not acknowledge update."));
    }
    messageQueue.declareFor(DocumentUpdatedMessage.class)
        .withTopic("scofu.document.updated." + request.collection())
        .push(new DocumentUpdatedMessage(request.collection(), request.json()));
    return CompletableFuture.completedFuture(new DocumentUpdateReply(true, null));
  }

  private CompletableFuture<DocumentDeleteReply> onDocumentDeleteRequest(
      DocumentDeleteRequest request) {
    final var collection = mongoDatabase.getCollection(request.collection());
    final var result = collection.deleteOne(Filters.eq("_id", request.id()));
    if (!result.wasAcknowledged()) {
      return CompletableFuture.completedFuture(
          new DocumentDeleteReply(false, "Database did not acknowledge deletion."));
    }
    messageQueue.declareFor(DocumentDeletedMessage.class)
        .withTopic("scofu.document.deleted." + request.collection())
        .push(new DocumentDeletedMessage(request.collection(), request.id()));
    return CompletableFuture.completedFuture(new DocumentDeleteReply(true, null));
  }
}
