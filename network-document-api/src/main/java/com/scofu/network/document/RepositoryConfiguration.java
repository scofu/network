package com.scofu.network.document;

import com.google.common.cache.CacheBuilder;
import com.scofu.network.document.internal.InternalRepositoryConfiguration;

/** Repository options. */
public interface RepositoryConfiguration {

  /** Creates and returns a new builder. */
  static Builder builder() {
    return new InternalRepositoryConfiguration.Builder();
  }

  /** Returns the collection. */
  String collection();

  /** Returns the cache builder. */
  CacheBuilder<Object, Object> cacheBuilder();

  /** Builder. */
  interface Builder {

    /**
     * Sets the collection.
     *
     * @param collection the collection
     */
    Builder withCollection(String collection);

    /**
     * Sets the cache builder.
     *
     * @param cacheBuilder the cache builder
     */
    Builder withCacheBuilder(CacheBuilder<Object, Object> cacheBuilder);

    /** Builds and returns a new repository configuration. */
    RepositoryConfiguration build();
  }
}
