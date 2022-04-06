package com.scofu.network.document;

import com.scofu.network.document.internal.InternalFilter;
import java.util.Map;

/**
 * https://docs.mongodb.com/manual/reference/method/db.collection.find/#db.collection.find--
 * <br>
 * https://docs.mongodb.com/manual/reference/operator/query/
 */
public interface Filter {

  /**
   * Creates and returns a new empty filter.
   *
   * @param <T> unused
   */
  static <T> Filter empty() {
    return InternalFilter.newInternalFilter(Map.of());
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  the key
   * @param t1  the value
   * @param <T> the type of the value
   */
  static <T> Filter where(String k1, T t1) {
    return InternalFilter.newInternalFilter(Map.of(k1, t1));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2) {
    return InternalFilter.newInternalFilter(Map.of(k1, t1, k2, t2));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3) {
    return InternalFilter.newInternalFilter(Map.of(k1, t1, k2, t2, k3, t3));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4) {
    return InternalFilter.newInternalFilter(Map.of(k1, t1, k2, t2, k3, t3, k4, t4));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param k5  fifth key
   * @param t5  fifth value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4,
      String k5, T t5) {
    return InternalFilter.newInternalFilter(Map.of(k1, t1, k2, t2, k3, t3, k4, t4, k5, t5));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param k5  fifth key
   * @param t5  fifth value
   * @param k6  sixth key
   * @param t6  sixth value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4,
      String k5, T t5, String k6, T t6) {
    return InternalFilter.newInternalFilter(Map.of(k1, t1, k2, t2, k3, t3, k4, t4, k5, t5, k6, t6));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param k5  fifth key
   * @param t5  fifth value
   * @param k6  sixth key
   * @param t6  sixth value
   * @param k7  seventh key
   * @param t7  seventh value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4,
      String k5, T t5, String k6, T t6, String k7, T t7) {
    return InternalFilter.newInternalFilter(
        Map.of(k1, t1, k2, t2, k3, t3, k4, t4, k5, t5, k6, t6, k7, t7));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param k5  fifth key
   * @param t5  fifth value
   * @param k6  sixth key
   * @param t6  sixth value
   * @param k7  seventh key
   * @param t7  seventh value
   * @param k8  eight key
   * @param t8  eight value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4,
      String k5, T t5, String k6, T t6, String k7, T t7, String k8, T t8) {
    return InternalFilter.newInternalFilter(
        Map.of(k1, t1, k2, t2, k3, t3, k4, t4, k5, t5, k6, t6, k7, t7, k8, t8));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param k5  fifth key
   * @param t5  fifth value
   * @param k6  sixth key
   * @param t6  sixth value
   * @param k7  seventh key
   * @param t7  seventh value
   * @param k8  eight key
   * @param t8  eight value
   * @param k9  ninth key
   * @param t9  ninth value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4,
      String k5, T t5, String k6, T t6, String k7, T t7, String k8, T t8, String k9, T t9) {
    return InternalFilter.newInternalFilter(
        Map.of(k1, t1, k2, t2, k3, t3, k4, t4, k5, t5, k6, t6, k7, t7, k8, t8, k9, t9));
  }

  /**
   * Creates a new where filter.
   *
   * @param k1  first key
   * @param t1  first value
   * @param k2  second key
   * @param t2  second value
   * @param k3  third key
   * @param t3  third value
   * @param k4  fourth key
   * @param t4  fourth value
   * @param k5  fifth key
   * @param t5  fifth value
   * @param k6  sixth key
   * @param t6  sixth value
   * @param k7  seventh key
   * @param t7  seventh value
   * @param k8  eight key
   * @param t8  eight value
   * @param k9  ninth key
   * @param t9  ninth value
   * @param k10 tenth key
   * @param t10 tenth value
   * @param <T> the type of the values
   */
  static <T> Filter where(String k1, T t1, String k2, T t2, String k3, T t3, String k4, T t4,
      String k5, T t5, String k6, T t6, String k7, T t7, String k8, T t8, String k9, T t9,
      String k10, T t10) {
    return InternalFilter.newInternalFilter(
        Map.of(k1, t1, k2, t2, k3, t3, k4, t4, k5, t5, k6, t6, k7, t7, k8, t8, k9, t9, k10, t10));
  }

  /**
   * Creates and returns a new '$exists' filter.
   *
   * @param yes yes
   */
  static Filter exists(boolean yes) {
    return where("$exists", yes);
  }

  /**
   * Merges this filter with the given filter.
   *
   * @param filter the filter
   */
  Filter and(Filter filter);

  /**
   * Creates and returns a new '$and' filter.
   *
   * @param first  first
   * @param second second
   */
  static Filter and(Filter first, Filter second) {
    return where("$and", new Filter[]{first, second});
  }

  /**
   * Creates and returns a new '$or' filter.
   *
   * @param first  first
   * @param second second
   */
  static Filter or(Filter first, Filter second) {
    return where("$or", new Filter[]{first, second});
  }

  /**
   * Creates and returns a new '$nor' filter.
   *
   * @param first  first
   * @param second second
   */
  static Filter nor(Filter first, Filter second) {
    return where("$nor", new Filter[]{first, second});
  }

  /**
   * Creates and returns a new '$not' filter.
   *
   * @param filter the filter
   */
  static Filter not(Filter filter) {
    return where("$not", filter);
  }

  /**
   * Creates and returns a new '$in' filter.
   *
   * @param values the values
   * @param <T>    the type of the values
   */
  @SafeVarargs
  static <T> Filter contains(T... values) {
    return where("$in", values);
  }

  /**
   * Creates and returns a new '$nin' filter.
   *
   * @param values the values
   * @param <T>    the type of the values
   */
  @SafeVarargs
  static <T> Filter containsNone(T... values) {
    return where("$nin", values);
  }

  /**
   * Creates and returns a new '$eq' filter.
   *
   * @param value the value
   * @param <T>   the type of the value
   */
  static <T> Filter equalsTo(T value) {
    return where("$eq", value);
  }

  /**
   * Creates and returns a new '$ne' filter.
   *
   * @param value the value
   * @param <T>   the type of the value
   */
  static <T> Filter doesNotEqualTo(T value) {
    return where("$ne", value);
  }

  /**
   * Creates and returns a new '$gt' filter.
   *
   * @param value the value
   * @param <T>   the type of the value
   */
  static <T extends Number> Filter isGreaterThan(T value) {
    return where("$gt", value);
  }

  /**
   * Creates and returns a new '$gte' filter.
   *
   * @param value the value
   * @param <T>   the type of the value
   */
  static <T extends Number> Filter isGreaterThanOrEqualTo(T value) {
    return where("$gte", value);
  }

  /**
   * Creates and returns a new '$lt' filter.
   *
   * @param value the value
   * @param <T>   the type of the value
   */
  static <T extends Number> Filter isLessThan(T value) {
    return where("$lt", value);
  }

  /**
   * Creates and returns a new '$lte' filter.
   *
   * @param value the value
   * @param <T>   the type of the value
   */
  static <T extends Number> Filter isLessThanOrEqualTo(T value) {
    return where("$lte", value);
  }

  /**
   * Creates and returns a new between filter.
   *
   * @param lowerBound the lower bound
   * @param upperBound the upper bound
   * @param <T>        the type of the value
   */
  static <T extends Number> Filter isBetween(T lowerBound, T upperBound) {
    return where("$gt", lowerBound, "$lt", upperBound);
  }

  /**
   * Creates and returns a new '$regex' filter.
   *
   * @param regex the regex
   */
  static Filter matchesRegex(String regex) {
    return where("$regex", regex);
  }

  /**
   * Creates and returns a new '$options' filter.
   *
   * @param options the options
   */
  static Filter withOptions(String options) {
    return where("$options", options);
  }

  /**
   * Creates and returns a new '$expr' filter.
   *
   * @param expression the expression
   */
  static Filter matchesExpression(String expression) {
    return where("$expr", expression);
  }

  /**
   * Creates and returns a new '$text' filter.
   *
   * @param textBuilder the textBuilder
   */
  static Filter matchesText(TextBuilder textBuilder) {
    return where("$text", textBuilder.build());
  }

  /**
   * Returns this filter as a map.
   */
  Map<String, Object> asMap();

}
