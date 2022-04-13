package com.scofu.network.instance;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;
import com.scofu.common.json.PeriodEscapedString;
import com.scofu.network.document.Document;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a network.
 */
public class Network implements Document {

  @JsonProperty("_id")
  private final String id;
  private final Map<String, Deployment> deployments;
  private final Map<PeriodEscapedString, String> endpoints;
  private String name;

  /**
   * Constructs a new network.
   *
   * @param id          the id
   * @param deployments the deployments
   * @param endpoints   the endpoints
   * @param name        the name
   */
  @JsonCreator
  public Network(@JsonProperty("_id") String id, Map<String, Deployment> deployments,
      Map<PeriodEscapedString, String> endpoints, String name) {
    this.id = id;
    this.deployments = deployments;
    this.endpoints = endpoints;
    this.name = name;
  }

  /**
   * Creates and returns a new builder.
   */
  public static NetworkBuilder builder() {
    return new NetworkBuilder();
  }

  @Override
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
   * Returns an optional deployment by the given endpoint.
   *
   * @param domain the domain
   */
  public Optional<Deployment> deploymentByEndpoint(String domain) {
    return Optional.ofNullable(endpoints.get(new PeriodEscapedString(domain)))
        .map(deployments::get);
  }

  /**
   * Returns the optional name.
   */
  public Optional<String> name() {
    return Optional.ofNullable(name);
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  @JsonIgnore
  public void setName(String name) {
    checkNotNull(name, "name");
    this.name = name;
  }
}
