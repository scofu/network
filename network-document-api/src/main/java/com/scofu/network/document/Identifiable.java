package com.scofu.network.document;

/**
 * Something identifiable.
 *
 * @param <T> the type of the identifier
 */
public interface Identifiable<T> {

  /**
   * Returns the identifier.
   */
  T id();

}
