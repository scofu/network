package com.scofu.network.document.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.scofu.network.document.Book;
import com.scofu.network.document.Document;
import com.scofu.network.document.DocumentNotFoundException;
import com.scofu.network.document.DocumentRepository;
import com.scofu.network.document.DocumentRepositoryException;
import com.scofu.network.document.Page;
import com.scofu.network.document.Query;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

final class InternalPage<D extends Document> implements Page<D> {

  private final Key key;
  private final LoadingCache<Key, Map<D, Integer>> cache;
  private final Book<D> parent;

  public InternalPage(
      DocumentRepository<D> repository, PageOptions pageOptions, Query query, Book<D> parent) {
    this(
        repository,
        new Key(
            pageOptions,
            query
                .edit()
                .skip(pageOptions.documentsToSkip())
                .limitTo(pageOptions.documentsPerPage())
                .build()),
        parent);
  }

  public InternalPage(DocumentRepository<D> repository, Key key, Book<D> parent) {
    this.key = key;
    this.cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(createCacheLoader(repository));
    this.parent = parent;
  }

  @Override
  public Map<D, Integer> documents() {
    parent.tryRefresh(false);
    try {
      return cache.get(key);
    } catch (ExecutionException | UncheckedExecutionException e) {
      if (e.getCause() instanceof DocumentNotFoundException) {
        return Map.of();
      }
      throw new DocumentRepositoryException("Cache error: ", e);
    }
  }

  @Override
  public void refresh() {
    cache.refresh(key);
  }

  private CacheLoader<Key, Map<D, Integer>> createCacheLoader(DocumentRepository<D> repository) {
    return new CacheLoader<>() {
      @Override
      public Map<D, Integer> load(Key key) throws Exception {
        final var index = new AtomicInteger(key.options.documentsToSkip());
        return repository
            .find(key.query)
            .thenApply(
                map ->
                    map.values().stream()
                        .collect(
                            Collectors.toMap(
                                Function.identity(),
                                d -> index.getAndIncrement(),
                                (x, y) -> x,
                                Maps::newLinkedHashMap)))
            .join();
      }
    };
  }

  /** Key that pairs options with query. */
  public record Key(PageOptions options, Query query) {}
}
