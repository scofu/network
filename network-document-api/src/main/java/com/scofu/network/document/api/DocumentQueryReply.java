package com.scofu.network.document.api;

import java.util.Map;

/**
 * Query reply.
 *
 * @param ok            if ok
 * @param error         the error
 * @param documentsById the documents by id
 */
public record DocumentQueryReply(boolean ok, String error, Map<String, String> documentsById) {}
