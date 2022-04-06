package com.scofu.network.document.api;

import com.scofu.network.document.Query;

/**
 * Count request.
 *
 * @param collection the collection
 * @param query      the query
 */
public record DocumentCountRequest(String collection, Query query) {}
