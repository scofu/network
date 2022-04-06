package com.scofu.network.document.internal;

/**
 * Represents the options of a page.
 */
public record PageOptions(int page, int documentsPerPage) {

  /**
   * Returns the amount of documents to skip before the current page.
   */
  public int documentsToSkip() {
    if (page > 0) {
      return (page - 1) * documentsPerPage;
    }
    return 0;
  }

}
