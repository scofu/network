package com.scofu.network.document;

/** Document not found exception. */
public class DocumentNotFoundException extends DocumentRepositoryException {

  public DocumentNotFoundException() {}

  public DocumentNotFoundException(String s) {
    super(s);
  }

  public DocumentNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public DocumentNotFoundException(Throwable cause) {
    super(cause);
  }
}
