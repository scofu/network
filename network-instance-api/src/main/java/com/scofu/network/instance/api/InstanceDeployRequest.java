package com.scofu.network.instance.api;

import com.scofu.network.instance.Deployment;

/**
 * Deploy request.
 *
 * @param networkId  the network id
 * @param deployment the deployment
 */
public record InstanceDeployRequest(String networkId, Deployment deployment) {}
