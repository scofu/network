package com.scofu.network.instance.api;

import com.scofu.network.instance.Instance;

/**
 * Instance availability reply.
 */
public record InstanceAvailabilityReply(boolean ok, String error, Instance instance) {}
