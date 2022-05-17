package com.scofu.network.instance;

import com.scofu.network.message.Result;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Resolves endpoints. */
public interface EndpointResolver {

  Result<Optional<Deployment>> resolveDeployment(InetSocketAddress address);

  Result<Optional<Motd>> resolveMotd(InetSocketAddress address);
}
