package com.scofu.network.message;

/**
 * Basic ping message.
 *
 * @param ns  the ns
 * @param tag the tag
 */
public record Ping(long ns, String tag) {

  /**
   * Returns a new ping with the given tag.
   *
   * @param tag the tag
   */
  public Ping withTag(String tag) {
    return new Ping(ns, tag);
  }

}
