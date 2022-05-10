package com.scofu.network.instance.api;

import com.scofu.network.instance.Instance;

/**
 * Instance deploy reply.
 */
public record InstanceDeployReply(boolean ok, String error, Instance instance) {}
