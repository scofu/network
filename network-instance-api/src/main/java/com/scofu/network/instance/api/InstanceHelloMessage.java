package com.scofu.network.instance.api;

import com.scofu.network.instance.Instance;
import java.util.UUID;

/**
 * Hello message.
 *
 * @param deploymentId the deployment id
 * @param instance     the instance
 */
public record InstanceHelloMessage(UUID deploymentId, Instance instance) {}
