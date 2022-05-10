package com.scofu.network.instance;

import static com.scofu.text.Components.centerWithSpaces;
import static net.kyori.adventure.text.Component.text;

import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.text.Characters;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

final class TestDynamicEndpointResolver implements EndpointResolver {

  private final LazyFactory lazyFactory;

  @Inject
  TestDynamicEndpointResolver(LazyFactory lazyFactory) {
    this.lazyFactory = lazyFactory;
  }

  @Override
  public CompletableFuture<Optional<Deployment>> resolveDeployment(InetSocketAddress address) {
    final var domain = address.getHostString().replaceFirst("mc\\.", "");
    if (!domain.endsWith(".dynamic.scofu.com")) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    final var target = domain.split("\\.dynamic\\.scofu\\.com", 2)[0];
    return CompletableFuture.completedFuture(Optional.of(lazyFactory.create(Deployment.class,
        Deployment::id,
        target,
        Deployment::name,
        target,
        Deployment::image,
        "docker.scofu.com/lobby")));
  }

  @Override
  public CompletableFuture<Optional<Motd>> resolveMotd(InetSocketAddress address) {
    final var domain = address.getHostString().replaceFirst("mc\\.", "");
    if (!domain.endsWith(".dynamic.scofu.com")) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    final var target = domain.split("\\.dynamic\\.scofu\\.com", 2)[0];
    final var top = centerWithSpaces(text("Scofu Network"), Locale.US, Characters.CHAT_WIDTH / 2);
    final var bottom = centerWithSpaces(text("dynamic -> " + target),
        Locale.US,
        Characters.CHAT_WIDTH / 2);
    return CompletableFuture.completedFuture(Optional.of(lazyFactory.create(Motd.class,
        Motd::top,
        top,
        Motd::bottom,
        bottom)));
  }
}
