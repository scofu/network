package com.scofu.network.document.api;

/**
 * Delete request.
 *
 * @param collection the collection
 * @param id         the id
 */
public record DocumentDeleteRequest(String collection, Object id) {}
