package com.scofu.network.document;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.scofu.network.document.Filter.where;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;
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
import com.scofu.network.message.QueueBuilder;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Abstract document repository implementation.
 *
 * @param <D> the type of the documents
 */
public class AbstractDocumentRepository<D extends Document> implements DocumentRepository<D> {

  private final String collection;
  private final Type type;
  private final Json json;
  private final QueueBuilder<DocumentQueryRequest, DocumentQueryReply> findQueue;
  private final QueueBuilder<DocumentCountRequest, DocumentCountReply> countQueue;
  private final QueueBuilder<DocumentUpdateRequest, DocumentUpdateReply> updateQueue;
  private final QueueBuilder<DocumentDeleteRequest, DocumentDeleteReply> deleteQueue;
  private final LoadingCache<String, D> cache;
  private final List<DocumentStateListener<D>> stateListeners;

  /**
   * Constructs a new abstract document repository.
   *
   * @param messageQueue            the message queue
   * @param messageFlow             the message flow
   * @param type                    the type
   * @param json                    the json
   * @param repositoryConfiguration the repository configuration
   */
  public AbstractDocumentRepository(MessageQueue messageQueue, MessageFlow messageFlow, Type type,
      Json json, RepositoryConfiguration repositoryConfiguration) {
    this.collection = repositoryConfiguration.collection();
    this.type = type;
    this.json = json;
    this.findQueue = messageQueue.declareFor(DocumentQueryRequest.class)
        .expectReply(DocumentQueryReply.class)
        .withTopic("scofu.document.query." + collection);
    this.countQueue = messageQueue.declareFor(DocumentCountRequest.class)
        .expectReply(DocumentCountReply.class)
        .withTopic("scofu.document.count." + collection);
    this.updateQueue = messageQueue.declareFor(DocumentUpdateRequest.class)
        .expectReply(DocumentUpdateReply.class)
        .withTopic("scofu.document.update." + collection);
    this.deleteQueue = messageQueue.declareFor(DocumentDeleteRequest.class)
        .expectReply(DocumentDeleteReply.class)
        .withTopic("scofu.document.delete." + collection);
    this.cache = createCache(repositoryConfiguration);
    this.stateListeners = Lists.newArrayList();
    messageFlow.subscribeTo(DocumentUpdatedMessage.class)
        .withTopic("scofu.document.updated." + collection)
        .via(this::onDocumentUpdatedMessage);
    messageFlow.subscribeTo(DocumentDeletedMessage.class)
        .withTopic("scofu.document.deleted." + collection)
        .via(this::onDocumentDeletedMessage);
  }

  @Override
  public LoadingCache<String, D> cache() {
    return cache;
  }

  @Override
  public Optional<D> byId(String id) {
    checkNotNull(id, "id");
    try {
      return Optional.of(cache.get(id));
    } catch (ExecutionException | UncheckedExecutionException e) {
      if (e.getCause() instanceof DocumentNotFoundException) {
        return Optional.empty();
      }
      throw new DocumentRepositoryException("Cache error: ", e);
    }
  }

  @Override
  public CompletableFuture<Optional<D>> byIdAsync(String id) {
    checkNotNull(id, "id");
    if (cache.asMap().containsKey(id)) {
      return CompletableFuture.completedFuture(Optional.of(cache.asMap().get(id)));
    }
    return findById(id).thenApplyAsync(optional -> optional.map(document -> {
      cache.put(id, document);
      return document;
    }));
  }

  @Override
  public CompletableFuture<Map<String, D>> find(Query query) {
    return findQueue.push(new DocumentQueryRequest(collection, query)).thenApplyAsync(reply -> {
      if (!reply.ok()) {
        throw new DocumentQueryException("Query error: " + reply.error());
      }
      return reply.documentsById()
          .entrySet()
          .stream()
          .collect(
              Collectors.toMap(Entry::getKey, e -> json.fromString(type, e.getValue()), (x, y) -> x,
                  Maps::newLinkedHashMap));
    });
  }

  @Override
  public CompletableFuture<Optional<D>> findById(String id) {
    return find(Query.builder().filter(where("_id", id)).limitTo(1).build()).thenApplyAsync(
        documents -> documents.values().stream().findFirst());
  }

  @Override
  public CompletableFuture<Long> count(Query query) {
    return countQueue.push(new DocumentCountRequest(collection, query)).thenApplyAsync(reply -> {
      if (!reply.ok()) {
        throw new DocumentQueryException("Count error: " + reply.error());
      }
      return reply.count();
    });
  }

  @Override
  public CompletableFuture<D> update(D document) {
    checkNotNull(document, "document");
    cache.put(document.id(), document);
    return updateQueue.push(new DocumentUpdateRequest(collection, json.toString(type, document)))
        .thenApplyAsync(reply -> {
          if (!reply.ok()) {
            throw new DocumentRepositoryException("Update error: " + reply.error());
          }
          return document;
        });
  }

  @Override
  public CompletableFuture<Void> delete(String id) {
    checkNotNull(id, "id");
    return deleteQueue.push(new DocumentDeleteRequest(collection, id)).thenApplyAsync(reply -> {
      if (!reply.ok()) {
        throw new DocumentRepositoryException("Delete error: " + reply.error());
      }
      return null;
    });
  }

  @Override
  public void addStateListener(DocumentStateListener<D> stateListener) {
    stateListeners.add(stateListener);
  }

  private LoadingCache<String, D> createCache(RepositoryConfiguration options) {
    return options.cacheBuilder().build(new CacheLoader<>() {
      @Override
      public D load(String key) throws Exception {
        return findById(key).join()
            .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + key));
      }
    });
  }

  private void onDocumentUpdatedMessage(DocumentUpdatedMessage message) {
    if (!message.collection().equals(collection)) {
      return;
    }
    final var document = json.<D>fromString(type, message.json());
    final var cached = cache.getIfPresent(document.id());
    if (cached == null) {
      // TODO: add option to not return here
      stateListeners.forEach(stateListener -> stateListener.onUpdate(document, false));
      return;
    }
    // TODO: update model fields instead?
    cache.put(document.id(), document);
    stateListeners.forEach(stateListener -> stateListener.onUpdate(document, true));
  }

  private void onDocumentDeletedMessage(DocumentDeletedMessage message) {
    if (!message.collection().equals(collection)) {
      return;
    }
    cache.invalidate(message.id());
    stateListeners.forEach(stateListener -> stateListener.onDelete((String) message.id()));
  }
}
