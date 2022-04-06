package com.scofu.network.document.api;

/**
 * Published by the document service when a document deletion has been acknowledged by the
 * database.
 *
 * @param collection the collection
 * @param id         the id
 */
public record DocumentDeletedMessage(String collection, Object id) {}
