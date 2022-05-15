package com.scofu.network.document;

import com.scofu.network.document.Page.Item;
import com.scofu.network.document.internal.InternalBook;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a 'book' of pages.
 *
 * <p>See {@link Page}.
 *
 * @param <D> the type of the document
 */
public interface Book<D extends Document> {

  /**
   * Creates and returns a new book.
   *
   * @param query the query
   * @param documentsPerPage the amount of documents per page
   * @param repository the repository
   * @param duration the duration between refreshes
   * @param <D> the type of the document
   */
  static <D extends Document> Book<D> of(
      Query query, int documentsPerPage, DocumentRepository<D> repository, Duration duration) {
    return InternalBook.newInternalBook(query, documentsPerPage, repository, duration);
  }

  /**
   * Returns the page at the given index.
   *
   * @param page the page
   */
  Page<D> page(int page);

  /** Returns the duration until the next refresh. */
  Duration durationUntilNextRefresh();

  /**
   * Tries to refresh the internal cache for every page.
   *
   * <p>The refresh will only happen if {@link Book#durationUntilNextRefresh()} is less than zero,
   * or if force is {@code true}.
   *
   * @param force whether to force it or not
   */
  void tryRefresh(boolean force);

  /**
   * Filters the items.
   *
   * @param predicate the predicate
   */
  void filter(Predicate<Item<D>> predicate);

  /**
   * Returns a new book with the given operator applied to the current query.
   *
   * @param function the function
   */
  Book<D> withQuery(Function<QueryBuilder, Query> function);
}
