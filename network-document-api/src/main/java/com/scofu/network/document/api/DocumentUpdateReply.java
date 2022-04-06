package com.scofu.network.document.api;

/**
 * Update reply.
 *
 * @param ok    if ok
 * @param error error
 */
public record DocumentUpdateReply(boolean ok, String error) {}
