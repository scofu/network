package com.scofu.network.instance.api;

import com.scofu.network.instance.Deployment;
import java.util.Map;

/**
 * Instance join request.
 */
public record InstanceAvailabilityRequest(Deployment deployment, Map<String, Object> context) {}
