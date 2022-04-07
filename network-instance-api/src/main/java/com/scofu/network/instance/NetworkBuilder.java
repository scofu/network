package com.scofu.network.instance;

import com.google.common.collect.Maps;
import com.scofu.common.json.PeriodEscapedString;
import java.util.Map;

/**
 * Builds networks.
 */
public class NetworkBuilder {

  private String id;
  private Map<String, Deployment> deployments;
  private Map<PeriodEscapedString, String> endpoints;
  private String name;

  /**
   * Sets the id.
   *
   * @param id the id
   */
  public NetworkBuilder withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Adds a deployment.
   *
   * @param id         the id
   * @param deployment the deployment
   */
  public NetworkBuilder withDeployment(String id, Deployment deployment) {
    if (deployments == null) {
      deployments = Maps.newHashMap();
    }
    deployments.put(id, deployment);
    return this;
  }

  /**
   * Adds an endpoint.
   *
   * @param domain       the domain
   * @param deploymentId the deployment id
   */
  public NetworkBuilder withEndpoint(String domain, String deploymentId) {
    if (endpoints == null) {
      endpoints = Maps.newHashMap();
    }
    endpoints.put(new PeriodEscapedString(domain), deploymentId);
    return this;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public NetworkBuilder withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns the id.
   */
  public String id() {
    return id;
  }

  /**
   * Returns the deployments.
   */
  public Map<String, Deployment> deployments() {
    return deployments;
  }

  /**
   * Returns the endpoints.
   */
  public Map<PeriodEscapedString, String> endpoints() {
    return endpoints;
  }

  /**
   * Returns the name.
   */
  public String name() {
    return name;
  }

  /**
   * Builds and returns a new network.
   */
  public Network build() {
    return new Network(id, deployments == null ? Map.of() : deployments,
        endpoints == null ? Map.of() : endpoints, name);
  }
}