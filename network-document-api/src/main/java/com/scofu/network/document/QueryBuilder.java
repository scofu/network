package com.scofu.network.document;

/** Builds queries. */
public class QueryBuilder {

  private Filter filter;
  private Filter sort;
  private int skip;
  private int limit;

  QueryBuilder(Query query) {
    this.filter = query.filter();
    this.sort = query.sort();
    this.skip = query.skip();
    this.limit = query.limit();
  }

  QueryBuilder() {}

  /**
   * Sets the filter.
   *
   * @param filter the filter
   */
  public QueryBuilder filter(Filter filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Sets the sort.
   *
   * @param sort the sort
   */
  public QueryBuilder sort(Filter sort) {
    this.sort = sort;
    return this;
  }

  /**
   * Sets the amount to skip.
   *
   * @param skip the amount to skip
   */
  public QueryBuilder skip(int skip) {
    this.skip = skip;
    return this;
  }

  /**
   * Sets the limit.
   *
   * @param limit the limit
   */
  public QueryBuilder limitTo(int limit) {
    this.limit = limit;
    return this;
  }

  /** Builds and returns a new query. */
  public Query build() {
    return new Query(filter, sort, skip, limit);
  }
}
