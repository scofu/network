package com.scofu.network.instance;

import static com.scofu.text.Components.center;
import static net.kyori.adventure.text.Component.text;

import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.text.Characters;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Final endpoint resolver.
 */
public class FinalEndpointResolver implements EndpointResolver {

  private final Set<EndpointResolver> resolvers;
  private final LazyFactory lazyFactory;

  @Inject
  FinalEndpointResolver(Set<EndpointResolver> resolvers, LazyFactory lazyFactory) {
    this.resolvers = resolvers;
    this.lazyFactory = lazyFactory;
  }

  @Override
  public CompletableFuture<Optional<Deployment>> resolveDeployment(InetSocketAddress address) {
    return CompletableFuture.supplyAsync(() -> resolvers.stream()
        .map(endpointResolver -> endpointResolver.resolveDeployment(address).join())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst());
  }

  @Override
  public CompletableFuture<Optional<Motd>> resolveMotd(InetSocketAddress address) {
    return CompletableFuture.supplyAsync(() -> resolvers.stream()
        .map(endpointResolver -> endpointResolver.resolveMotd(address).join())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .or(() -> Optional.of(lazyFactory.create(Motd.class, Motd::top,
            center(text("Scofu :^)"), Locale.US, Characters.CHAT_SPACING / 2), Motd::bottom,
            center(text("Unknown endpoint!"), Locale.US, Characters.CHAT_SPACING / 2)))));
  }
}
