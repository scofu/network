package com.scofu.network.document;

import com.jsoniter.annotation.JsonProperty;

/**
 * Text.
 *
 * @param search             the search
 * @param language           the language
 * @param caseSensitive      whether it is case-sensitive or not
 * @param diacriticSensitive whether it is diacritic-sensitive or not
 */
public record Text(@JsonProperty("$search") String search,
                   @JsonProperty("$language") String language,
                   @JsonProperty("$caseSensitive") boolean caseSensitive,
                   @JsonProperty("$diacriticSensitive") boolean diacriticSensitive) {

  /**
   * Creates and returns a new builder.
   *
   * @param search the search
   */
  public static TextBuilder search(String search) {
    return new TextBuilder(search);
  }
}
