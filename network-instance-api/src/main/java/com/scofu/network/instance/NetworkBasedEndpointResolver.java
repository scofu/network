package com.scofu.network.instance;

import static com.scofu.text.Components.centerWithSpaces;
import static com.scofu.text.EntryComponent.entry;
import static net.kyori.adventure.text.Component.text;

import com.scofu.common.json.PeriodEscapedString;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.message.Result;
import com.scofu.text.Characters;
import com.scofu.text.Color;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;

final class NetworkBasedEndpointResolver implements EndpointResolver {

  private final NetworkRepository networkRepository;
  private final LazyFactory lazyFactory;

  @Inject
  NetworkBasedEndpointResolver(NetworkRepository networkRepository, LazyFactory lazyFactory) {
    this.networkRepository = networkRepository;

    this.lazyFactory = lazyFactory;
  }

  @Override
  public Result<Optional<Deployment>> resolveDeployment(InetSocketAddress address) {
    final var domain = address.getHostString().replaceFirst("mc\\.", "");
    final var escapedDomain = new PeriodEscapedString(domain);
    return networkRepository
        .findByDomain(domain)
        .apply(Optional::map, (Network network) -> network.deployments().get(escapedDomain));
  }

  @Override
  public Result<Optional<Motd>> resolveMotd(InetSocketAddress address) {
    final var domain = address.getHostString().replaceFirst("mc\\.", "");
    return networkRepository
        .findByDomain(domain)
        .apply(Optional::map, (Network network) -> createMotd(network, domain));
  }

  private Motd createMotd(Network network, String domain) {
    final var top = entry("Scofu | %s", network.id());
    final var bottom = text(domain).color(Color.BRIGHT_BLACK);
    return lazyFactory.create(
        Motd.class,
        Motd::top,
        centerWithSpaces(top, Locale.US, Characters.MOTD_WIDTH),
        Motd::bottom,
        centerWithSpaces(bottom, Locale.US, Characters.MOTD_WIDTH));
  }
}
