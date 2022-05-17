package com.scofu.network.instance;

import static com.scofu.text.Components.centerWithSpaces;
import static com.scofu.text.EntryComponent.entry;
import static net.kyori.adventure.text.Component.text;

import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.message.Result;
import com.scofu.text.Characters;
import com.scofu.text.Color;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

/** Final endpoint resolver. */
public class FinalEndpointResolver implements EndpointResolver {

  private final Set<EndpointResolver> resolvers;
  private final LazyFactory lazyFactory;

  @Inject
  FinalEndpointResolver(Set<EndpointResolver> resolvers, LazyFactory lazyFactory) {
    this.resolvers = resolvers;
    this.lazyFactory = lazyFactory;
  }

  @Override
  public Result<Optional<Deployment>> resolveDeployment(InetSocketAddress address) {
    return Result.of(
        () ->
            resolvers.stream()
                .map(endpointResolver -> endpointResolver.resolveDeployment(address).join())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst());
  }

  @Override
  public Result<Optional<Motd>> resolveMotd(InetSocketAddress address) {
    return Result.of(
        () ->
            resolvers.stream()
                .map(endpointResolver -> endpointResolver.resolveMotd(address).join())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .or(() -> Optional.of(createMotd())));
  }

  private Motd createMotd() {
    final var top = text("Scofu :^)").color(Color.BRIGHT_GREEN);
    final var bottom = entry("Unknown endpoint!");
    return lazyFactory.create(
        Motd.class,
        Motd::top,
        centerWithSpaces(top, Locale.US, Characters.MOTD_WIDTH),
        Motd::bottom,
        centerWithSpaces(bottom, Locale.US, Characters.MOTD_WIDTH));
  }
}
