package com.scofu.network.instance.bukkit;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.Json;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Group;
import com.scofu.network.instance.GroupRepository;
import com.scofu.network.instance.Instance;
import com.scofu.network.instance.api.InstanceAvailabilityReply;
import com.scofu.network.instance.api.InstanceAvailabilityRequest;
import com.scofu.network.instance.api.InstanceGoodbyeMessage;
import com.scofu.network.instance.api.InstanceHelloMessage;
import com.scofu.network.instance.api.InstanceStatusReply;
import com.scofu.network.instance.api.InstanceStatusRequest;
import com.scofu.network.instance.api.InstanceStatusUpdateMessage;
import com.scofu.network.instance.bukkit.event.AvailabilityCheckEvent;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.QueueBuilder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.inject.Named;
import org.bukkit.plugin.Plugin;

final class LocalInstanceAnnouncer implements Feature {

  private final GroupRepository groupRepository;
  private final LocalAvailability localAvailability;
  private final Plugin plugin;
  private final QueueBuilder<InstanceHelloMessage, Void> helloQueue;
  private final QueueBuilder<InstanceGoodbyeMessage, Void> goodbyeQueue;
  private final QueueBuilder<InstanceStatusUpdateMessage, Void> statusQueue;
  private final CompletableFuture<Instance> instanceFuture;
  private final Json json;
  private final InetAddress localHost;

  @Inject
  LocalInstanceAnnouncer(MessageQueue messageQueue, MessageFlow messageFlow,
      GroupRepository groupRepository, LocalAvailability localAvailability, Plugin plugin,
      Json json, @Named("LocalHost") InetAddress localHost) {
    this.helloQueue = messageQueue.declareFor(InstanceHelloMessage.class)
        .withTopic("scofu.instance.hello");
    this.goodbyeQueue = messageQueue.declareFor(InstanceGoodbyeMessage.class)
        .withTopic("scofu.instance.goodbye");
    this.statusQueue = messageQueue.declareFor(InstanceStatusUpdateMessage.class)
        .withTopic("scofu.instance.status");
    this.groupRepository = groupRepository;
    this.localAvailability = localAvailability;
    this.plugin = plugin;
    this.instanceFuture = new CompletableFuture<>();
    this.json = json;
    this.localHost = localHost;
    messageFlow.subscribeTo(InstanceStatusRequest.class)
        .replyWith(InstanceStatusReply.class)
        .withTopic("scofu.instance.status." + localHost.getHostName())
        .via(this::onInstanceStatusRequest);
    messageFlow.subscribeTo(InstanceAvailabilityRequest.class)
        .replyWith(InstanceAvailabilityReply.class)
        .withTopic("scofu.instance.availability." + localHost.getHostName())
        .via(this::onInstanceAvailabilityRequest);
  }

  @Override
  public void enable() {
    final var deploymentIdString = System.getenv("INSTANCE_DEPLOYMENT_ID");
    if (deploymentIdString == null) {
      System.out.println("Not deployed by network service?");
      return;
    }
    final var deployment = json.fromString(Deployment.class, System.getenv("INSTANCE_DEPLOYMENT"));
    final var deploymentId = UUID.fromString(deploymentIdString);
    final var instance = new Instance(localHost.getHostName(), deployment,
        new InetSocketAddress(localHost, 25565));
    final var group = groupRepository.byId(deployment.groupId())
        .orElseGet(() -> new Group(deployment.groupId()));
    group.instancePlayerCountMap().put(instance.id(), 0);
    groupRepository.update(group).thenAccept(x -> {
      helloQueue.push(new InstanceHelloMessage(deploymentId, instance));
      instanceFuture.complete(instance);
    });
    //    statusQueue.push(new InstanceStatusUpdateMessage(instance, localAvailability.get()));
  }

  @Override
  public void disable() {
    final var deployment = json.fromString(Deployment.class, System.getenv("INSTANCE_DEPLOYMENT"));
    goodbyeQueue.push(new InstanceGoodbyeMessage(new Instance(localHost.getHostName(), deployment,
        new InetSocketAddress(localHost, 25565))));
  }

  private CompletableFuture<InstanceStatusReply> onInstanceStatusRequest(
      InstanceStatusRequest request) {
    if (instanceFuture.isDone()) {
      return completedFuture(
          new InstanceStatusReply(instanceFuture.join(), localAvailability.get()));
    }
    return instanceFuture.thenApply(
        instance -> new InstanceStatusReply(instance, localAvailability.get()));

  }

  private CompletableFuture<InstanceAvailabilityReply> onInstanceAvailabilityRequest(
      InstanceAvailabilityRequest request) {
    final var event = new AvailabilityCheckEvent(request.context());
    plugin.getServer().getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return completedFuture(null);
    }
    final var deployment = json.fromString(Deployment.class, System.getenv("INSTANCE_DEPLOYMENT"));
    final var instance = new Instance(localHost.getHostName(), deployment,
        new InetSocketAddress(localHost, 25565));
    return completedFuture(
        new InstanceAvailabilityReply(true, null, instance, localAvailability.get()));
  }
}
