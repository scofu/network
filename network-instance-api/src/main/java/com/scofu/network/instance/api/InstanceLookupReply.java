package com.scofu.network.instance.api;

import com.scofu.network.instance.Instance;
import java.util.List;

/**
 * Lookup reply.
 *
 * @param instances the instances
 */
public record InstanceLookupReply(List<Instance> instances) {}
