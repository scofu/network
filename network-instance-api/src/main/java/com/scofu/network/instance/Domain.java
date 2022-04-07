package com.scofu.network.instance;

import java.util.Objects;

/**
 * Represents a domain.
 */
@SuppressWarnings("ClassCanBeRecord")
public class Domain {

  private final String string;

  private Domain(String string) {
    this.string = string;
  }

  /**
   * Creates and returns a new domain.
   *
   * @param string the string
   */
  public static Domain domain(String string) {
    return new Domain(string);
  }

  /**
   * Returns the string.
   */
  public String string() {
    return string;
  }

  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof Domain domain && string.equals(domain.string);
  }

  @Override
  public int hashCode() {
    return Objects.hash(string);
  }
}
