package com.scofu.network.document.api;

/**
 * Update request.
 *
 * @param collection the collection
 * @param json       the json
 */
public record DocumentUpdateRequest(String collection, String json) {}
