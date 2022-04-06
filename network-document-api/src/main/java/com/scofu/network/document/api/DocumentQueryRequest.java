package com.scofu.network.document.api;

import com.scofu.network.document.Query;

/**
 * Query request.
 *
 * @param collection the collection
 * @param query      the query
 */
public record DocumentQueryRequest(String collection, Query query) {}
