package com.scofu.network.document;

import com.google.common.cache.Cache;
import com.scofu.network.message.Result;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents a repository of identifiable objects.
 *
 * @param <T> the type of the identifier
 * @param <I> the type of the objects
 */
public interface Repository<T, I extends Identifiable<T>> {

  /** Returns the cache. */
  Cache<T, I> cache();

  /**
   * Returns an optional object by the given identifier.
   *
   * <p>If the object isn't cached, it will be queried and cached through {@link
   * Repository#findById(Object)}.
   *
   * @param id the identifier
   */
  Optional<I> byId(T id);

  /**
   * Returns an optional object by the given identifier async.
   *
   * <p>If the object isn't cached, it will be queried and cached through {@link
   * Repository#findById(Object)}.
   *
   * @param id the identifier
   */
  Result<Optional<I>> byIdAsync(T id);

  /**
   * Returns an optional object, either the first one in the cache matching the given filter, or the
   * first one matching the given query.
   *
   * <p>If it is queried and matched there, it will be cached.
   *
   * @param filter the filter
   * @param querySupplier the query supplier
   */
  Result<Optional<I>> fromCacheOrQuery(
      Predicate<I> filter, Supplier<Query> querySupplier);

  /**
   * Creates a new request with the given query.
   *
   * <p>This will not cache anything.
   *
   * @param query the query
   */
  Result<Map<String, I>> find(Query query);

  /**
   * Queries for any object with the given identifier.
   *
   * <p>This will not cache anything.
   *
   * @param id the identifier
   */
  Result<Optional<I>> findById(T id);

  /**
   * Requests the amount of objects matching the given query.
   *
   * @param query the query
   */
  Result<Long> count(Query query);

  /**
   * Updates the given object.
   *
   * <p>This will update it in the cache as well.
   *
   * @param i the object
   */
  Result<I> update(I i);

  /**
   * Deletes any object with the given identifier.
   *
   * <p>This will invalidate it from the cache as well.
   *
   * @param id the identifier
   */
  Result<Void> delete(T id);
}
