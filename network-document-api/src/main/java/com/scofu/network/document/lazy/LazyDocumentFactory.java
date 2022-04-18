package com.scofu.network.document.lazy;

import com.jsoniter.any.Any;

/**
 * Creates lazy documents.
 */
public interface LazyDocumentFactory {

  /**
   * Creates and returns a new lazy document.
   *
   * @param type the type
   * @param id   the id
   * @param any  the any
   * @param <T>  the type of the document
   */
  <T extends LazyDocument> T create(Class<T> type, String id, Any any);

  /**
   * Creates and returns a new lazy document.
   *
   * @param type the type
   * @param id   the id
   * @param <T>  the type of the document
   */
  <T extends LazyDocument> T create(Class<T> type, String id);
}
