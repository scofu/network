package com.scofu.network.document.internal;

import com.google.common.cache.CacheBuilder;
import com.scofu.network.document.RepositoryConfiguration;

/**
 * Internal repository configuration.
 */
public class InternalRepositoryConfiguration implements RepositoryConfiguration {

  private final String collection;
  private final CacheBuilder<Object, Object> cacheBuilder;

  private InternalRepositoryConfiguration(String collection,
      CacheBuilder<Object, Object> cacheBuilder) {
    this.collection = collection;
    this.cacheBuilder = cacheBuilder;
  }

  @Override
  public String collection() {
    return collection;
  }

  @Override
  public CacheBuilder<Object, Object> cacheBuilder() {
    return cacheBuilder;
  }

  /**
   * Internal builder.
   */
  public static class Builder implements RepositoryConfiguration.Builder {

    private String collection;
    private CacheBuilder<Object, Object> cacheBuilder;

    @Override
    public Builder withCollection(String collection) {
      this.collection = collection;
      return this;
    }

    @Override
    public Builder withCacheBuilder(CacheBuilder<Object, Object> cacheBuilder) {
      this.cacheBuilder = cacheBuilder;
      return this;
    }

    @Override
    public RepositoryConfiguration build() {
      return new InternalRepositoryConfiguration(collection, cacheBuilder);
    }
  }
}
