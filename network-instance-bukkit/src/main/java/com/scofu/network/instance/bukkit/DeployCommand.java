package com.scofu.network.instance.bukkit;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.inject.Inject;
import com.scofu.command.model.Expansion;
import com.scofu.command.model.Identified;
import com.scofu.command.validation.Permission;
import com.scofu.common.inject.Feature;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.api.InstanceDeployReply;
import com.scofu.network.instance.api.InstanceDeployRequest;
import com.scofu.network.instance.api.InstanceNavigateReply;
import com.scofu.network.instance.api.InstanceNavigateRequest;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.QueueBuilder;
import java.util.Locale;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

final class DeployCommand implements Feature {

  private final MessageQueue messageQueue;
  private final QueueBuilder<InstanceDeployRequest, InstanceDeployReply> deployQueue;
  private final QueueBuilder<InstanceNavigateRequest, InstanceNavigateReply> navigateQueue;

  @Inject
  DeployCommand(MessageQueue messageQueue) {
    this.messageQueue = messageQueue;
    this.deployQueue = messageQueue.declareFor(InstanceDeployRequest.class)
        .expectReply(InstanceDeployReply.class)
        .withTopic("scofu.instance.deploy");
    this.navigateQueue = messageQueue.declareFor(InstanceNavigateRequest.class)
        .expectReply(InstanceNavigateReply.class)
        .withTopic("scofu.instance.navigate");
  }

  @Identified("deploy")
  @Permission("scofu.command.deploy")
  private void deploy(Expansion<Player> source, String image, String mapGroupId,
      String mapArtifactId) {
    final var player = source.orElseThrow();
    player.sendMessage(text("Deploying...").color(NamedTextColor.GRAY));
    final var deployment = Deployment.builder()
        .withNetworkId("internal")
        .withGroupId("internal")
        .withImage(image)
        .withName("test-" + player.getName().toLowerCase(Locale.ROOT).replaceAll("_", "-"))
        .withEnvironment("APP_WORLD_GROUP_ID", mapGroupId)
        .withEnvironment("APP_WORLD_ARTIFACT_ID", mapArtifactId)
        .build();
    deployQueue.push(new InstanceDeployRequest("internal", deployment))
        .whenComplete((reply, throwable) -> {
          if (throwable != null) {
            player.sendMessage(translatable("Error, something went wrong."));
          } else if (!reply.ok()) {
            player.sendMessage(text("Error: " + reply.error()));
          } else {
            if (!player.isOnline()) {
              return;
            }
            player.sendMessage(text("Connecting...").color(NamedTextColor.GRAY));
            navigateQueue.push(
                    new InstanceNavigateRequest(player.getUniqueId(), reply.instance().id()))
                .whenComplete(((navigateReply, navigateThrowable) -> {
                  if (!navigateReply.ok()) {
                    player.sendMessage(text("Error connecting: " + navigateReply.error()).color(
                        NamedTextColor.RED));
                  }
                }));
          }
        });
  }

}
