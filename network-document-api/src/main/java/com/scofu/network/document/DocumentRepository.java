package com.scofu.network.document;

/**
 * Repository of documents.
 *
 * @param <D> the type of the documents
 */
public interface DocumentRepository<D extends Document> extends Repository<String, D> {

  /**
   * Adds the given state listener.
   *
   * @param stateListener the state listener
   */
  void addStateListener(DocumentStateListener<D> stateListener);
}
