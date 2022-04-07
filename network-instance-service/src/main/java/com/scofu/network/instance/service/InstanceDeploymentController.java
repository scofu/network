package com.scofu.network.instance.service;

import static java.util.function.Predicate.not;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.Json;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.Instance;
import com.scofu.network.instance.api.InstanceDeployReply;
import com.scofu.network.instance.api.InstanceDeployRequest;
import com.scofu.network.instance.api.InstanceGoodbyeMessage;
import com.scofu.network.instance.api.InstanceHelloMessage;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class InstanceDeploymentController implements Feature {

  private final Map<UUID, CompletableFuture<InstanceDeployReply>> pendingDeploymentsByUniqueId;
  private final Map<String, CompletableFuture<InstanceDeployReply>> pendingDeploymentsByGroupId;
  private final ConcurrentKubernetesClient concurrentClient;
  private final Json json;
  private final List<EnvVar> environment;
  private final Pod pod;

  @Inject
  InstanceDeploymentController(MessageQueue messageQueue, MessageFlow messageFlow, Json json) {
    this.json = json;
    this.pendingDeploymentsByUniqueId = Maps.newConcurrentMap();
    this.pendingDeploymentsByGroupId = Maps.newConcurrentMap();
    this.concurrentClient = ConcurrentKubernetesClient.of();
    messageFlow.subscribeTo(InstanceDeployRequest.class)
        .replyWith(InstanceDeployReply.class)
        .withTopic("scofu.instance.deploy")
        .via(this::onInstanceDeployRequest);
    messageFlow.subscribeTo(InstanceHelloMessage.class)
        .withTopic("scofu.instance.hello")
        .via(this::onInstanceHelloMessage);
    this.environment = Lists.newArrayList(new EnvVarBuilder().withName("RABBITMQ_HOST")
        .withValue(System.getenv("RABBITMQ_HOST"))
        .build(), new EnvVarBuilder().withName("RABBITMQ_PORT")
        .withValue(System.getenv("RABBITMQ_PORT"))
        .build(), new EnvVarBuilder().withName("RABBITMQ_USERNAME")
        .withValue(System.getenv("RABBITMQ_USERNAME"))
        .build(), new EnvVarBuilder().withName("RABBITMQ_PASSWORD")
        .withValue(System.getenv("RABBITMQ_PASSWORD"))
        .build(), new EnvVarBuilder().withName("RABBITMQ_EXCHANGE")
        .withValue(System.getenv("RABBITMQ_EXCHANGE"))
        .build(), new EnvVarBuilder().withName("NEXUS_USERNAME")
        .withValue(System.getenv("NEXUS_USERNAME"))
        .build(), new EnvVarBuilder().withName("NEXUS_PASSWORD")
        .withValue(System.getenv("NEXUS_PASSWORD"))
        .build());
    this.pod = new PodBuilder().withNewMetadata()
        .withNamespace("default")
        .withLabels(Map.of("app", "minecraft"))
        .endMetadata()
        .withNewSpec()
        .addNewContainer()
        .withName("container")
        .withImagePullPolicy("Always")
        .endContainer()
        .withImagePullSecrets(new LocalObjectReference("scofu-docker-hosted-test"))
        .withRestartPolicy("Never")
        .endSpec()
        .build();

    final var goodbyeQueue = messageQueue.declareFor(InstanceGoodbyeMessage.class)
        .withTopic("scofu.instance.goodbye");

    new Thread(() -> {
      try (var client = new DefaultKubernetesClient()) {
        client.pods()
            .inNamespace("default")
            .withLabel("app", "minecraft")
            .list()
            .getItems()
            .stream()
            .forEach(pod -> {
              System.out.println("yup");
            });

        client.pods()
            .inNamespace("default")
            .withLabel("app", "minecraft")
            .watch(new Watcher<Pod>() {
              @Override
              public void eventReceived(Action action, Pod resource) {
                System.out.println(action.name() + " - " + Optional.ofNullable(resource)
                    .map(Pod::getMetadata)
                    .map(ObjectMeta::getName)
                    .orElse("N/A"));

                switch (action) {
                  case MODIFIED -> {
                    System.out.println("PHASE IS: " + Optional.ofNullable(resource)
                        .map(Pod::getStatus)
                        .map(PodStatus::getPhase)
                        .orElse("NULL"));
                    Optional.ofNullable(resource)
                        .map(Pod::getStatus)
                        .map(PodStatus::getContainerStatuses)
                        .map(List::stream)
                        .flatMap(Stream::findFirst)
                        .map(ContainerStatus::getState)
                        .map(ContainerState::getTerminated)
                        .map(ContainerStateTerminated::getReason)
                        .ifPresent(reason -> {
                          if (reason.equals("Completed")) {
                            client.pods().delete(resource);
                            final var instanceId = resource.getMetadata().getName();
                            goodbyeQueue.push(
                                new InstanceGoodbyeMessage(new Instance(instanceId, null, null)));
                          }
                        });
                  }

                  case DELETED -> {
                    System.out.println("removing " + Optional.ofNullable(resource)
                        .map(Pod::getMetadata)
                        .map(ObjectMeta::getName)
                        .orElse("N/A"));
                    Optional.ofNullable(resource)
                        .map(Pod::getMetadata)
                        .map(ObjectMeta::getName)
                        .ifPresent(instanceId -> {
                          System.out.println("removing instance " + instanceId);
                          goodbyeQueue.push(
                              new InstanceGoodbyeMessage(new Instance(instanceId, null, null)));
                        });
                  }

                  default -> {
                  }
                }

              }

              @Override
              public void onClose(WatcherException cause) {

              }
            });
        while (!Thread.currentThread().isInterrupted()) {
          try {
            TimeUnit.SECONDS.sleep(1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Watcher interrupted...");
          }
        }
      }
    }).start();
  }

  private CompletableFuture<InstanceDeployReply> onInstanceDeployRequest(
      InstanceDeployRequest request) {
    final var groupedKey = request.deployment().groupId() + request.deployment().name();
    if (pendingDeploymentsByGroupId.containsKey(groupedKey)) {
      return pendingDeploymentsByGroupId.get(groupedKey);
    }
    final var future = new CompletableFuture<InstanceDeployReply>();
    final var deploymentId = UUID.randomUUID();
    pendingDeploymentsByUniqueId.put(deploymentId, future);
    pendingDeploymentsByGroupId.put(groupedKey, future);
    concurrentClient.accept(client -> {
      final var environment = Lists.newArrayList(
          new EnvVarBuilder().withName("INSTANCE_DEPLOYMENT_ID")
              .withValue(deploymentId.toString())
              .build(), new EnvVarBuilder().withName("INSTANCE_DEPLOYMENT")
              .withValue(json.toString(Deployment.class, request.deployment()))
              .build());
      environment.addAll(this.environment);
      Optional.ofNullable(request.deployment().environment())
          .filter(not(Map::isEmpty))
          .stream()
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .map(entry -> new EnvVarBuilder().withName(entry.getKey())
              .withValue(entry.getValue())
              .build())
          .collect(Collectors.toCollection(() -> environment));
      final var pod = new PodBuilder(this.pod).editMetadata()
          .withGenerateName(String.format("%s-", request.deployment().name()))
          .endMetadata()
          .editSpec()
          .editFirstContainer()
          .withImage(request.deployment().image())
          .withEnv(environment)
          .endContainer()
          .endSpec()
          .build();
      client.pods().inNamespace("default").create(pod);
    });

    return future;
  }

  private void onInstanceHelloMessage(InstanceHelloMessage message) {
    Optional.ofNullable(pendingDeploymentsByUniqueId.remove(message.deploymentId()))
        .ifPresent(future -> {
          future.complete(new InstanceDeployReply(true, null, message.instance()));
          pendingDeploymentsByGroupId.remove(
              message.instance().deployment().groupId() + message.instance().deployment().name());
        });
  }
}
