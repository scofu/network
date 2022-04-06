package com.scofu.network.document;

/**
 * Document exception.
 */
public class DocumentException extends IllegalStateException {

  public DocumentException() {
  }

  public DocumentException(String s) {
    super(s);
  }

  public DocumentException(String message, Throwable cause) {
    super(message, cause);
  }

  public DocumentException(Throwable cause) {
    super(cause);
  }
}
