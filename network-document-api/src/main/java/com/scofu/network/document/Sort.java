package com.scofu.network.document;

/** https://docs.mongodb.com/manual/reference/method/cursor.sort/#cursor.sort-- */
public interface Sort {

  /**
   * Creates and returns a new sort filter.
   *
   * @param field the field
   * @param order the order
   */
  static Filter by(String field, Order order) {
    return Filter.where(field, order.value());
  }

  /**
   * Creates and returns a new sort filter.
   *
   * @param field the field
   */
  static Filter by(String field) {
    return by(field, Order.LOWEST_TO_HIGHEST);
  }
}
