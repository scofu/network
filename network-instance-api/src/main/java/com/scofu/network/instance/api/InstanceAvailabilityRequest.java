package com.scofu.network.instance.api;

import java.util.Map;

/**
 * Availability request.
 *
 * @param groupId the group id
 * @param context the context
 */
public record InstanceAvailabilityRequest(String groupId, Map<String, Object> context) {}
