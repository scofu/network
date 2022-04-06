package com.scofu.network.document;

/**
 * Document repository exception.
 */
public class DocumentRepositoryException extends DocumentException {

  public DocumentRepositoryException() {
  }

  public DocumentRepositoryException(String s) {
    super(s);
  }

  public DocumentRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public DocumentRepositoryException(Throwable cause) {
    super(cause);
  }
}
