package com.scofu.network.instance;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Builds deployments.
 */
public class DeploymentBuilder {

  private String image;
  private String name;
  private String networkId = "internal";
  private String groupId = "internal";
  private Map<String, String> environment;

  /**
   * Sets the image.
   *
   * @param image the image
   */
  public DeploymentBuilder withImage(String image) {
    this.image = image;
    return this;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public DeploymentBuilder withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the network id.
   *
   * @param networkId the network id
   */
  public DeploymentBuilder withNetworkId(String networkId) {
    this.networkId = networkId;
    return this;
  }

  /**
   * Sets the group id.
   *
   * @param groupId the group id
   */
  public DeploymentBuilder withGroupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Adds the environment.
   *
   * @param key   the key
   * @param value the value
   */
  public DeploymentBuilder withEnvironment(String key, String value) {
    if (environment == null) {
      environment = Maps.newHashMap();
    }
    environment.put(key, value);
    return this;
  }

  /**
   * Builds and returns a new deployment.
   */
  public Deployment build() {
    return new Deployment(image, name, networkId, groupId, environment);
  }
}