package com.scofu.network.document;

/**
 * Builds text.
 */
public class TextBuilder {

  private final String search;
  private String language;
  private boolean caseSensitive;
  private boolean diacriticSensitive;

  /**
   * Constructs a new text builder.
   *
   * @param search the search
   */
  public TextBuilder(String search) {
    this.search = search;
  }

  /**
   * Sets the language.
   *
   * @param language the language
   */
  public TextBuilder language(String language) {
    this.language = language;
    return this;
  }

  /**
   * Sets whether it is case-sensitive or not.
   *
   * @param caseSensitive whether it is case-sensitive or not.
   */
  public TextBuilder caseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /**
   * Sets it to case-sensitive.
   */
  public TextBuilder caseSensitive() {
    this.caseSensitive = true;
    return this;
  }

  /**
   * Sets whether it is diacritic-sensitive or not.
   *
   * @param diacriticSensitive whether it is diacritic-sensitive or not.
   */
  public TextBuilder diacriticSensitive(boolean diacriticSensitive) {
    this.diacriticSensitive = diacriticSensitive;
    return this;
  }

  /**
   * Sets it to diacritic-sensitive.
   */
  public TextBuilder diacriticSensitive() {
    this.diacriticSensitive = true;
    return this;
  }

  Text build() {
    return new Text(search, language, caseSensitive, diacriticSensitive);
  }
}