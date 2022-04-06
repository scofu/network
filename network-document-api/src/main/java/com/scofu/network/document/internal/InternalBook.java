package com.scofu.network.document.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.scofu.network.document.Book;
import com.scofu.network.document.Document;
import com.scofu.network.document.DocumentRepository;
import com.scofu.network.document.DocumentRepositoryException;
import com.scofu.network.document.Page;
import com.scofu.network.document.Page.Item;
import com.scofu.network.document.Query;
import com.scofu.network.document.QueryBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Internal book.
 *
 * @param <D> the type of the documents
 */
public class InternalBook<D extends Document> implements Book<D> {

  private final Query query;
  private final int documentsPerPage;
  private final DocumentRepository<D> repository;
  private final LoadingCache<Integer, Page<D>> cache;
  private final Duration duration;
  private transient long refreshAtMillis;

  private InternalBook(Query query, int documentsPerPage, DocumentRepository<D> repository,
      Duration duration) {
    this.query = query;
    this.documentsPerPage = documentsPerPage;
    this.repository = repository;
    this.duration = duration;
    this.cache = CacheBuilder.newBuilder()
        .weakValues()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build(createCacheLoader(query, documentsPerPage, repository));
    this.refreshAtMillis = Instant.now().plus(duration).toEpochMilli();
  }

  public static <D extends Document> InternalBook<D> newInternalBook(Query query,
      int documentsPerPage, DocumentRepository<D> repository, Duration duration) {
    return new InternalBook<>(query, documentsPerPage, repository, duration);
  }

  @Override
  public Page<D> page(int page) {
    try {
      return cache.get(page);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw new DocumentRepositoryException("Cache error: ", e);
    }
  }

  @Override
  public Duration durationUntilNextRefresh() {
    tryRefresh(false);
    return Duration.ofMillis(refreshAtMillis - System.currentTimeMillis());
  }

  @Override
  public void tryRefresh(boolean force) {
    if (force) {
      refreshAtMillis = System.currentTimeMillis() + duration.toMillis();
      cache.asMap().values().forEach(Page::refresh);
      return;
    }
    if (System.currentTimeMillis() > refreshAtMillis) {
      synchronized (this) {
        if (System.currentTimeMillis() > refreshAtMillis) {
          refreshAtMillis = System.currentTimeMillis() + duration.toMillis();
          cache.asMap().values().forEach(Page::refresh);
        }
      }
    }
  }

  @Override
  public void filter(Predicate<Item<D>> predicate) {

  }

  @Override
  public Book<D> withQuery(Function<QueryBuilder, Query> function) {
    return new InternalBook<>(function.apply(query.edit()), documentsPerPage, repository, duration);
  }

  private CacheLoader<Integer, Page<D>> createCacheLoader(Query query, int documentsPerPage,
      DocumentRepository<D> repository) {
    return new CacheLoader<>() {
      @Override
      public Page<D> load(Integer key) throws Exception {
        return new InternalPage<>(repository, new PageOptions(key, documentsPerPage), query,
            InternalBook.this);
      }
    };
  }
}
