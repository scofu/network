package com.scofu.network.instance;

import static com.scofu.text.Components.center;
import static net.kyori.adventure.text.Component.text;

import com.scofu.common.json.PeriodEscapedString;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.text.Characters;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
  public CompletableFuture<Optional<Deployment>> resolveDeployment(InetSocketAddress address) {
    final var domain = address.getHostString().replaceFirst("mc\\.", "");
    return networkRepository.findByDomain(domain)
        .thenApplyAsync(o -> o.map(network -> network.deployments()
            .get(new PeriodEscapedString(domain))));
  }

  @Override
  public CompletableFuture<Optional<Motd>> resolveMotd(InetSocketAddress address) {
    final var domain = address.getHostString().replaceFirst("mc\\.", "");
    return networkRepository.findByDomain(domain)
        .thenApplyAsync(o -> o.map(network -> lazyFactory.create(Motd.class,
            Motd::top,
            center(text("Scofu :^)"), Locale.US, Characters.CHAT_SPACING / 2),
            Motd::bottom,
            center(text("network: " + network.id()), Locale.US, Characters.CHAT_SPACING / 2))));
  }
}
