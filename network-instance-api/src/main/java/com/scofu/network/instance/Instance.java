package com.scofu.network.instance;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Represents an instance.
 *
 * @param id         the id
 * @param deployment the deployment
 * @param address    the address
 */
public record Instance(String id, Deployment deployment, InetSocketAddress address) {

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof Instance instance && id.equals(instance.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
