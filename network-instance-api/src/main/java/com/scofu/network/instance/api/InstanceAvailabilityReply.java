package com.scofu.network.instance.api;

import com.scofu.network.instance.Availability;
import com.scofu.network.instance.Instance;

/**
 * Availability reply.
 *
 * @param ok           if ok
 * @param error        the error
 * @param instance     the instance
 * @param availability the availability
 */
public record InstanceAvailabilityReply(boolean ok, String error, Instance instance,
                                        Availability availability) {}
