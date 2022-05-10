package com.scofu.network.instance.bukkit;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.scofu.common.json.Json;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Instance;
import com.scofu.network.instance.InstanceRepository;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Local instance provider.
 */
public class LocalInstanceProvider {

  private final InetAddress localHost;
  private final InstanceRepository instanceRepository;
  private final LazyFactory lazyFactory;
  private final Json json;

  @Inject
  LocalInstanceProvider(@Named("LocalHost") InetAddress localHost,
      InstanceRepository instanceRepository, LazyFactory lazyFactory, Json json) {
    this.localHost = localHost;
    this.instanceRepository = instanceRepository;
    this.lazyFactory = lazyFactory;
    this.json = json;
  }

  /**
   * Returns the instance.
   */
  public CompletableFuture<Instance> get() {
    return instanceRepository.byIdAsync(localHost.getHostName())
        .thenComposeAsync(optional -> optional.map(CompletableFuture::completedFuture)
            .orElseGet(() -> {
              final var deployment = json.fromString(Deployment.class,
                  System.getenv("INSTANCE_DEPLOYMENT"));
              final var instance = lazyFactory.create(Instance.class,
                  Instance::id,
                  localHost.getHostName(),
                  Instance::deployment,
                  deployment,
                  Instance::address,
                  new InetSocketAddress(localHost, 25565));
              return instanceRepository.update(instance);
            }));
  }
}
