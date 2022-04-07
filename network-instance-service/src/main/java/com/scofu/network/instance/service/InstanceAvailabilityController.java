package com.scofu.network.instance.service;

import static java.util.concurrent.CompletableFuture.anyOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.document.Query;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.GroupRepository;
import com.scofu.network.instance.Instance;
import com.scofu.network.instance.api.InstanceAvailabilityReply;
import com.scofu.network.instance.api.InstanceAvailabilityRequest;
import com.scofu.network.instance.api.InstanceGoodbyeMessage;
import com.scofu.network.instance.api.InstanceLookupReply;
import com.scofu.network.instance.api.InstanceLookupRequest;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

final class InstanceAvailabilityController implements Feature {

  //  private final QueueBuilder<InstanceStatusRequest, InstanceStatusReply> statusQueue;
  private final GroupRepository groupRepository;
  private final MessageQueue messageQueue;

  @Inject
  InstanceAvailabilityController(MessageQueue messageQueue, MessageFlow messageFlow,
      GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
    this.messageQueue = messageQueue;
    //    this.statusQueue = messageQueue.declareFor(InstanceStatusRequest.class)
    //        .expectReply(InstanceStatusReply.class)
    //        .withTopic("scofu.instance.status");
    messageFlow.subscribeTo(InstanceAvailabilityRequest.class)
        .replyWith(InstanceAvailabilityReply.class)
        .withTopic("scofu.instance.availability")
        .via(this::onInstanceAvailabilityRequest);
    messageFlow.subscribeTo(InstanceGoodbyeMessage.class)
        .withTopic("scofu.instance.goodbye")
        .via(this::onInstanceGoodbyeMessage);
    //    messageFlow.subscribeTo(InstanceStatusUpdateMessage.class)
    //        .withTopic("scofu.instance.status")
    //        .via(this::handleStatus);
    messageFlow.subscribeTo(InstanceLookupRequest.class)
        .replyWith(InstanceLookupReply.class)
        .withTopic("scofu.instance.lookup")
        .via(this::onInstanceLookupRequest);
  }

  @Override
  public void enable() {
    new Thread(() -> {
      try (var client = new DefaultKubernetesClient()) {
        final var pods = client.pods()
            .inNamespace("default")
            .withLabel("app", "minecraft")
            .list()
            .getItems()
            .stream()
            .map(pod -> pod.getMetadata().getName())
            .toList();
        groupRepository.find(Query.empty()).whenComplete((groups, throwable) -> {
          if (throwable != null) {
            throwable.printStackTrace();
            return;
          }
          System.out.println("PODS: " + pods);
          System.out.println("GROUPS: " + groups.keySet().stream().toList());
          try {
            for (var group : groups.values()) {
              final var oldSize = group.instancePlayerCountMap().size();
              group.instancePlayerCountMap()
                  .keySet()
                  .stream()
                  .filter(Predicate.not(pods::contains))
                  .toList()
                  .forEach(group.instancePlayerCountMap()::remove);
              if (group.instancePlayerCountMap().size() != oldSize) {
                System.out.println("Updating group: " + group);
                groupRepository.update(group);
              }
            }
          } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ERROR");
          }
        });
      }
    }).start();
  }

  private CompletableFuture<InstanceLookupReply> onInstanceLookupRequest(
      InstanceLookupRequest request) {
    return supplyAsync(() -> {
      try (var client = new DefaultKubernetesClient()) {
        return new InstanceLookupReply(client.pods()
            .inNamespace("default")
            .withLabel("app", "minecraft")
            .list()
            .getItems()
            .stream()
            .map(pod -> {
              System.out.println(
                  "pod: " + pod.getMetadata().getName() + " ip: " + pod.getStatus().getPodIP());
              return new Instance(pod.getMetadata().getName(), null,
                  new InetSocketAddress(pod.getStatus().getPodIP(), 25565));
            })
            .toList());
      }
    });
  }

  private CompletableFuture<InstanceAvailabilityReply> onInstanceAvailabilityRequest(
      InstanceAvailabilityRequest request) {
    return supplyAsync(() -> {
      final var group = groupRepository.byId(request.groupId()).orElse(null);

      if (group == null) {
        return completedFuture(new InstanceAvailabilityReply(true, null, null, null));
      }

      final var future = new CompletableFuture<InstanceAvailabilityReply>();
      anyOf(group.instancePlayerCountMap()
          .keySet()
          .stream()
          .map(instanceId -> messageQueue.declareFor(InstanceAvailabilityRequest.class)
              .expectReply(InstanceAvailabilityReply.class)
              .withTopic("scofu.instance.availability." + instanceId)
              .push(request))
          .toArray(CompletableFuture[]::new)).thenApply(
              o -> o == null ? null : (InstanceAvailabilityReply) o)
          .completeOnTimeout(null, 5, TimeUnit.SECONDS)
          .whenComplete((availabilityReply, throwable) -> {
            if (throwable != null) {
              throwable.printStackTrace();
              future.complete(
                  new InstanceAvailabilityReply(false, "service exception", null, null));
              return;
            }
            future.complete(Objects.requireNonNullElseGet(availabilityReply,
                () -> new InstanceAvailabilityReply(true, null, null, null)));
          });
      return future;
    }).thenCompose(Function.identity());
  }

  private void onInstanceGoodbyeMessage(InstanceGoodbyeMessage message) {
    Optional.ofNullable(message.instance().deployment())
        .map(Deployment::groupId)
        .flatMap(groupRepository::byId)
        .ifPresent(group -> {
          group.instancePlayerCountMap().remove(message.instance().id());
          groupRepository.update(group);
        });
  }

}
