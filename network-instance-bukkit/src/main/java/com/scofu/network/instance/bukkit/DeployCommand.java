package com.scofu.network.instance.bukkit;

import static com.scofu.text.ContextualizedComponent.error;
import static com.scofu.text.ContextualizedComponent.info;

import com.google.inject.Inject;
import com.scofu.command.model.Identified;
import com.scofu.command.validation.Permission;
import com.scofu.common.Expansion;
import com.scofu.common.inject.Feature;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.instance.Deployment;
import com.scofu.network.instance.InstanceRepository;
import com.scofu.network.message.Result;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;

final class DeployCommand implements Feature {

  private final InstanceRepository instanceRepository;
  private final LazyFactory lazyFactory;

  @Inject
  DeployCommand(InstanceRepository instanceRepository, LazyFactory lazyFactory) {
    this.instanceRepository = instanceRepository;
    this.lazyFactory = lazyFactory;
  }

  @Identified("deploy")
  @Permission("scofu.command.deploy")
  private void deploy(
      Expansion<Player> source, String image, String mapGroupId, String mapArtifactId) {
    final var player = source.orElseThrow();
    final var name = player.getName().toLowerCase(Locale.ROOT).replaceAll("_", "-");
    final var deployment =
        lazyFactory.create(
            Deployment.class,
            Map.of(
                Deployment::id,
                "internal-" + name,
                Deployment::name,
                "test-" + name,
                Deployment::image,
                image,
                Deployment::environment,
                Map.of("APP_WORLD_GROUP_ID", mapGroupId, "APP_WORLD_ARTIFACT_ID", mapArtifactId)));
    info().text("Deploying...").prefixed().render(player::sendMessage);
    instanceRepository
        .deploy(deployment)
        .filter(x -> player.isOnline())
        .flatMap(
            reply -> {
              if (!reply.ok()) {
                System.out.println("deploy error: " + reply.error());
                error()
                    .text("Error deploying: %s.", reply.error())
                    .prefixed()
                    .render(player::sendMessage);
                return Result.empty();
              }
              return instanceRepository.connect(List.of(player.getUniqueId()), reply.instance());
            })
        .filterNotEmpty()
        .accept(
            reply -> {
              if (!reply.ok()) {
                System.out.println("connect error: " + reply.error());
                error()
                    .text("Error connecting: %s.", reply.error())
                    .prefixed()
                    .render(player::sendMessage);
              }
            });
  }
}
