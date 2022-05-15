package com.scofu.network.document;

/**
 * Listens for document state changes.
 *
 * @param <D> the type of the document
 */
public interface DocumentStateListener<D extends Document> {

  /**
   * Called when the given document is updated.
   *
   * @param d the document
   * @param cached whether it is currently cached or not
   */
  default void onUpdate(D d, boolean cached) {}

  /**
   * Called when a document with the given id is deleted.
   *
   * @param id the id
   */
  default void onDelete(String id) {}
}
