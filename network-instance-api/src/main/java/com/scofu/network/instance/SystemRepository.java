package com.scofu.network.instance;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scofu.common.json.Json;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.document.AbstractDocumentRepository;
import com.scofu.network.document.RepositoryConfiguration;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** System repository. */
public class SystemRepository extends AbstractDocumentRepository<System> {

  private final LazyFactory lazyFactory;

  @Inject
  SystemRepository(
      MessageQueue messageQueue, MessageFlow messageFlow, Json json, LazyFactory lazyFactory) {
    super(
        messageQueue,
        messageFlow,
        System.class,
        json,
        RepositoryConfiguration.builder()
            .withCollection("scofu.systems")
            .withCacheBuilder(CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS))
            .build());
    this.lazyFactory = lazyFactory;
  }

  /** Returns the system. */
  public CompletableFuture<System> get() {
    return this.byIdAsync(System.ID)
        .thenComposeAsync(
            system ->
                system
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(
                        () ->
                            update(
                                lazyFactory.create(
                                    System.class,
                                    System::id,
                                    System.ID,
                                    System::theme,
                                    "Vanilla"))));
  }
}
