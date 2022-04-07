package com.scofu.network.instance.api;

import com.scofu.network.instance.Instance;

/**
 * Deploy reply.
 *
 * @param ok       if ok
 * @param error    the error
 * @param instance the instance
 */
public record InstanceDeployReply(boolean ok, String error, Instance instance) {}
