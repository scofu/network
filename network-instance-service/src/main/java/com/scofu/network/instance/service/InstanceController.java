package com.scofu.network.instance.service;

import static java.util.function.Predicate.not;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.Json;
import com.scofu.network.document.Query;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.InstanceRepository;
import com.scofu.network.instance.api.InstanceCreatedMessage;
import com.scofu.network.instance.api.InstanceDeployReply;
import com.scofu.network.instance.api.InstanceDeployRequest;
import com.scofu.network.message.MessageFlow;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

final class InstanceController implements Feature {

  private final ConcurrentKubernetesClient concurrentClient;
  private final InstanceRepository instanceRepository;
  private final Json json;
  private final List<EnvVar> environment;
  private final Pod templatePod;
  private final Map<String, CompletableFuture<InstanceDeployReply>> pendingDeployments;

  @Inject
  InstanceController(MessageFlow messageFlow, InstanceRepository instanceRepository, Json json) {
    this.concurrentClient = ConcurrentKubernetesClient.of();
    this.instanceRepository = instanceRepository;
    this.json = json;
    this.environment = Lists.newArrayList(new EnvVarBuilder().withName("RABBITMQ_HOST")
            .withValue(System.getenv("RABBITMQ_HOST"))
            .build(),
        new EnvVarBuilder().withName("RABBITMQ_PORT")
            .withValue(System.getenv("RABBITMQ_PORT"))
            .build(),
        new EnvVarBuilder().withName("RABBITMQ_USERNAME")
            .withValue(System.getenv("RABBITMQ_USERNAME"))
            .build(),
        new EnvVarBuilder().withName("RABBITMQ_PASSWORD")
            .withValue(System.getenv("RABBITMQ_PASSWORD"))
            .build(),
        new EnvVarBuilder().withName("RABBITMQ_EXCHANGE")
            .withValue(System.getenv("RABBITMQ_EXCHANGE"))
            .build(),
        new EnvVarBuilder().withName("NEXUS_USERNAME")
            .withValue(System.getenv("NEXUS_USERNAME"))
            .build(),
        new EnvVarBuilder().withName("NEXUS_PASSWORD")
            .withValue(System.getenv("NEXUS_PASSWORD"))
            .build());
    this.templatePod = new PodBuilder().withNewMetadata()
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
    this.pendingDeployments = Maps.newConcurrentMap();
    messageFlow.subscribeTo(InstanceDeployRequest.class)
        .replyWith(InstanceDeployReply.class)
        .withTopic("scofu.instance")
        .via(this::onInstanceDeployRequest);
    messageFlow.subscribeTo(InstanceCreatedMessage.class)
        .withTopic("scofu.instance")
        .via(this::onInstanceCreatedMessage);
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
        instanceRepository.find(Query.empty()).thenAcceptAsync(instances -> {
          instances.values().forEach(instance -> {
            if (!pods.contains(instance.id())) {
              instanceRepository.delete(instance.id());
            }
          });
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
                            instanceRepository.delete(instanceId);
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
                          instanceRepository.delete(instanceId);
                        });
                  }

                  default -> {
                  }
                }
              }

              @Override
              public void onClose(WatcherException cause) {}
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
    final var pendingDeployment = pendingDeployments.get(request.deployment().id());
    if (pendingDeployment != null) {
      return pendingDeployment;
    }
    final var future = new CompletableFuture<InstanceDeployReply>();
    pendingDeployments.put(request.deployment().id(), future);
    concurrentClient.accept(client -> {
      final var environment = Lists.newArrayList(new EnvVarBuilder().withName("INSTANCE_DEPLOYMENT")
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
      final var pod = new PodBuilder(templatePod).editMetadata()
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

  private void onInstanceCreatedMessage(InstanceCreatedMessage message) {
    Optional.ofNullable(pendingDeployments.remove(message.instance().deployment().id()))
        .ifPresent(future -> future.complete(new InstanceDeployReply(true,
            null,
            message.instance())));
  }

}
