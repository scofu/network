package com.scofu.network.instance.bukkit;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.Instance;
import com.scofu.network.instance.InstanceRepository;
import com.scofu.network.instance.SystemRepository;
import com.scofu.network.instance.api.InstanceAvailabilityReply;
import com.scofu.network.instance.api.InstanceAvailabilityRequest;
import com.scofu.network.instance.api.InstanceCreatedMessage;
import com.scofu.network.instance.bukkit.event.AvailabilityCheckEvent;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.Queue;
import com.scofu.network.message.Result;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import javax.inject.Named;
import org.bukkit.plugin.Plugin;

final class LocalInstanceAnnouncer implements Feature {

  private final Plugin plugin;
  private final Queue<InstanceCreatedMessage, Void> aliveQueue;
  private final CompletableFuture<Instance> instanceFuture;
  private final InstanceRepository instanceRepository;
  private final LocalInstanceProvider instanceProvider;
  private final SystemRepository systemRepository;
  private final InetAddress localHost;

  @Inject
  LocalInstanceAnnouncer(
      MessageQueue messageQueue,
      MessageFlow messageFlow,
      Plugin plugin,
      InstanceRepository instanceRepository,
      LocalInstanceProvider instanceProvider,
      SystemRepository systemRepository,
      @Named("LocalHost") InetAddress localHost) {
    this.aliveQueue =
        messageQueue.declareFor(InstanceCreatedMessage.class).withTopic("scofu.instance");
    this.plugin = plugin;
    this.instanceRepository = instanceRepository;
    this.instanceProvider = instanceProvider;
    this.systemRepository = systemRepository;
    this.instanceFuture = new CompletableFuture<>();
    this.localHost = localHost;
    messageFlow
        .subscribeTo(InstanceAvailabilityRequest.class)
        .replyWith(InstanceAvailabilityReply.class)
        .withTopic("scofu.instance." + localHost.getHostName())
        .via(this::onInstanceAvailabilityRequest);
  }

  @Override
  public void enable() {
    System.out.println("getting instance");
    systemRepository
        .get()
        .flatMap(system -> instanceProvider.get())
        .accept(
            instance -> {
              instanceFuture.complete(instance);
              aliveQueue.push(new InstanceCreatedMessage(instance));
            });
  }

  @Override
  public void disable() {
    instanceRepository.delete(localHost.getHostName());
  }

  private Result<InstanceAvailabilityReply> onInstanceAvailabilityRequest(
      InstanceAvailabilityRequest request) {
    final var event = new AvailabilityCheckEvent(request.context());
    plugin.getServer().getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return Result.empty();
    }
    return instanceProvider
        .get()
        .map(instance -> new InstanceAvailabilityReply(true, null, instance));
  }
}
