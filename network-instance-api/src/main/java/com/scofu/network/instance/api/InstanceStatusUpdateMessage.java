package com.scofu.network.instance.api;

import com.scofu.network.instance.Availability;
import com.scofu.network.instance.Instance;

/**
 * Status update message.
 *
 * @param instance     the instance
 * @param availability the availability
 */
public record InstanceStatusUpdateMessage(Instance instance, Availability availability) {}
