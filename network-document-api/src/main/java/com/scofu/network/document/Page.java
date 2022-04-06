package com.scofu.network.document;

import java.util.Map;

/**
 * Represents a limited section of a larger collection of documents.
 *
 * <p>See {@link Book}.
 *
 * @param <D> the type of the documents
 */
public interface Page<D extends Document> {

  /**
   * Returns the documents.
   */
  Map<D, Integer> documents();

  /**
   * Refreshes the cache.
   *
   * <p>See {@link Book#tryRefresh(boolean)}.
   */
  void refresh();

  /**
   * Represents an item.
   *
   * @param index    the index
   * @param document the document
   * @param <D>      the type of the document
   */
  record Item<D extends Document>(int index, D document) {}
}
