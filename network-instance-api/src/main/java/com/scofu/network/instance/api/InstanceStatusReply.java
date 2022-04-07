package com.scofu.network.instance.api;

import com.scofu.network.instance.Availability;
import com.scofu.network.instance.Instance;

/**
 * Status reply.
 *
 * @param instance     the instance
 * @param availability the availability
 */
public record InstanceStatusReply(Instance instance, Availability availability) {}
