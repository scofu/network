package com.scofu.network.instance;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves endpoints.
 */
public interface EndpointResolver {

  CompletableFuture<Optional<Deployment>> resolveDeployment(InetSocketAddress address);

  CompletableFuture<Optional<Motd>> resolveMotd(InetSocketAddress address);

}
