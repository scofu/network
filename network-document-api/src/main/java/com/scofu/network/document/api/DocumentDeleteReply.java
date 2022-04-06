package com.scofu.network.document.api;

/**
 * Delete reply.
 *
 * @param ok    if ok
 * @param error the error
 */
public record DocumentDeleteReply(boolean ok, String error) {}
