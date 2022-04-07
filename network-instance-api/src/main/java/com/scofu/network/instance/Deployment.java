package com.scofu.network.instance;

import java.util.Map;

/**
 * Represents a deployment.
 *
 * @param image       the image
 * @param name        the name
 * @param networkId   the network id
 * @param groupId     the group id
 * @param environment the environment
 */
public record Deployment(String image, String name, String networkId, String groupId,
                         Map<String, String> environment) {

  /**
   * Creates and returns a new builder.
   */
  public static DeploymentBuilder builder() {
    return new DeploymentBuilder();
  }
}
