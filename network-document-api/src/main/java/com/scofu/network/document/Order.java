package com.scofu.network.document;

/** Field sorting order. */
public enum Order {
  LOWEST_TO_HIGHEST(1),
  HIGHEST_TO_LOWEST(-1);
  private final int value;

  Order(int value) {
    this.value = value;
  }

  /** Returns the value. */
  public int value() {
    return value;
  }
}
