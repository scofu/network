package com.scofu.network.document;

/** Document query exception. */
public class DocumentQueryException extends DocumentException {

  public DocumentQueryException() {}

  public DocumentQueryException(String s) {
    super(s);
  }

  public DocumentQueryException(String message, Throwable cause) {
    super(message, cause);
  }

  public DocumentQueryException(Throwable cause) {
    super(cause);
  }
}
