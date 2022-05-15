package com.scofu.network.instance;

import static com.scofu.network.document.Filter.exists;
import static com.scofu.network.document.Filter.where;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scofu.common.json.Json;
import com.scofu.common.json.Periods;
import com.scofu.network.document.AbstractDocumentRepository;
import com.scofu.network.document.Query;
import com.scofu.network.document.RepositoryConfiguration;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Network repository. */
public class NetworkRepository extends AbstractDocumentRepository<Network> {

  private final MessageQueue messageQueue;

  @Inject
  NetworkRepository(MessageQueue messageQueue, MessageFlow messageFlow, Json json) {
    super(
        messageQueue,
        messageFlow,
        Network.class,
        json,
        RepositoryConfiguration.builder()
            .withCollection("scofu.networks")
            .withCacheBuilder(CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS))
            .build());
    this.messageQueue = messageQueue;
  }

  /**
   * Queries and returns an optional network by the given domain.
   *
   * @param domain the domain
   */
  public CompletableFuture<Optional<Network>> findByDomain(String domain) {
    return find(Query.builder()
            .filter(where("deployments." + Periods.escape(domain), exists(true)))
            .limitTo(1)
            .build())
        .thenApply(map -> map.values().stream().findFirst());
  }
}
