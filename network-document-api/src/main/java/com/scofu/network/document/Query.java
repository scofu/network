package com.scofu.network.document;

/**
 * Represents a query.
 */
public record Query(Filter filter, Filter sort, int skip, int limit) {

  /**
   * Creates and returns a new builder.
   */
  public static QueryBuilder query() {
    return new QueryBuilder();
  }

  /**
   * Creates and returns a new empty query.
   */
  public static Query empty() {
    return new Query(null, null, 0, 0);
  }

  /**
   * Returns a new builder from this query.
   */
  public QueryBuilder edit() {
    return new QueryBuilder(this);
  }
}
