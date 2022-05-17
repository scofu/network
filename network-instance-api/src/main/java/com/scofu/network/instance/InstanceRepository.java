package com.scofu.network.instance;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scofu.common.json.Json;
import com.scofu.network.document.AbstractDocumentRepository;
import com.scofu.network.document.RepositoryConfiguration;
import com.scofu.network.instance.api.InstanceAvailabilityReply;
import com.scofu.network.instance.api.InstanceAvailabilityRequest;
import com.scofu.network.instance.api.InstanceConnectReply;
import com.scofu.network.instance.api.InstanceConnectRequest;
import com.scofu.network.instance.api.InstanceDeployReply;
import com.scofu.network.instance.api.InstanceDeployRequest;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.Queue;
import com.scofu.network.message.Result;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Instance repository. */
public class InstanceRepository extends AbstractDocumentRepository<Instance> {

  private final MessageQueue messageQueue;
  private final Queue<InstanceDeployRequest, InstanceDeployReply> deployQueue;
  private final Queue<InstanceConnectRequest, InstanceConnectReply> connectQueue;
  private final Queue<InstanceAvailabilityRequest, InstanceAvailabilityReply> availabilityQueue;

  @Inject
  InstanceRepository(MessageQueue messageQueue, MessageFlow messageFlow, Json json) {
    super(
        messageQueue,
        messageFlow,
        Instance.class,
        json,
        RepositoryConfiguration.builder()
            .withCollection("scofu.instances")
            .withCacheBuilder(CacheBuilder.newBuilder())
            .build());
    this.messageQueue = messageQueue;
    this.deployQueue =
        messageQueue
            .declareFor(InstanceDeployRequest.class)
            .expectReply(InstanceDeployReply.class)
            .withTopic("scofu.instance");
    this.availabilityQueue =
        messageQueue
            .declareFor(InstanceAvailabilityRequest.class)
            .expectReply(InstanceAvailabilityReply.class)
            .withTopic("scofu.instance");
    this.connectQueue =
        messageQueue
            .declareFor(InstanceConnectRequest.class)
            .expectReply(InstanceConnectReply.class)
            .withTopic("scofu.instance");
  }

  /**
   * Deploy.
   *
   * @param deployment the deployment
   */
  public Result<InstanceDeployReply> deploy(Deployment deployment) {
    return deployQueue.push(new InstanceDeployRequest(deployment));
  }

  /**
   * Check availability.
   *
   * @param deployment the deployment
   * @param context the context
   */
  public Result<InstanceAvailabilityReply> checkAvailability(
      Deployment deployment, Map<String, Object> context) {
    return availabilityQueue.push(new InstanceAvailabilityRequest(deployment, context));
  }

  /**
   * Connect.
   *
   * @param playerIds the player ids
   * @param instance the instance
   */
  public Result<InstanceConnectReply> connect(List<UUID> playerIds, Instance instance) {
    return connectQueue.push(new InstanceConnectRequest(playerIds, instance));
  }
}
