package com.scofu.network.document.api;

/**
 * Count reply.
 *
 * @param ok    if ok
 * @param error the error
 * @param count the count
 */
public record DocumentCountReply(boolean ok, String error, long count) {}
