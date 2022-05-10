package com.scofu.network.instance.api;

import com.scofu.network.instance.Instance;
import java.util.List;
import java.util.UUID;

/**
 * Instance connect request.
 */
public record InstanceConnectRequest(List<UUID> playerIds, Instance instance) {}
