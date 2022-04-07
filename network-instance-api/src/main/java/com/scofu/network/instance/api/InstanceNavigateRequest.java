package com.scofu.network.instance.api;

import java.util.UUID;

/**
 * Navigate request.
 *
 * @param id         the id
 * @param instanceId the instance id
 */
public record InstanceNavigateRequest(UUID id, String instanceId) {}
