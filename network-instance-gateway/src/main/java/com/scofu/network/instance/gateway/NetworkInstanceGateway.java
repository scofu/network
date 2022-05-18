package com.scofu.network.instance.gateway;


import com.google.common.collect.Maps;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.optifine.OptifineSupport;
import net.minestom.server.instance.ChunkGenerator;
import net.minestom.server.instance.ChunkPopulator;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.Biome.Category;
import net.minestom.server.world.biomes.Biome.Precipitation;
import net.minestom.server.world.biomes.BiomeEffects;

/** Network instance gateway. */
public class NetworkInstanceGateway extends Service {

  public static final Biome BIOME =
      Biome.builder()
          .category(Category.NONE)
          .name(NamespaceID.from("scofu:gateway"))
          .temperature(0.0F)
          .downfall(0.5F)
          .depth(0.125F)
          .scale(0.05F)
          .precipitation(Precipitation.SNOW)
          .effects(
              BiomeEffects.builder()
                  //          .fogColor(0x3e4b80)
                  .fogColor(0x827e75)
                  //          .skyColor(0x7700ff)
                  .skyColor(0x45decf)
                  .waterColor(0x0076E4)
                  .waterFogColor(0x000533)
                  .build())
          .build();
  public static final DimensionType DIMENSION_TYPE =
      DimensionType.builder(NamespaceID.from("scofu:gateway"))
          .ultrawarm(false)
          .natural(true)
          .piglinSafe(false)
          .respawnAnchorSafe(false)
          .bedSafe(true)
          .raidCapable(true)
          .skylightEnabled(true)
          .ceilingEnabled(false)
          .fixedTime(null)
          .ambientLight(15.0f)
          .height(256)
          .logicalHeight(256)
          .infiniburn(NamespaceID.from("minecraft:infiniburn_overworld"))
          .build();

  public static void main(String[] args) {
    load(Stage.PRODUCTION, new NetworkInstanceGateway());
  }

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
  }

  @Override
  public void enable() {
    final var minecraftServer = MinecraftServer.init();
    MinecraftServer.setBrandName("Scofu Gateway");
    OptifineSupport.enable();
    BungeeCordProxy.enable();

    MinecraftServer.getBiomeManager().addBiome(BIOME);
    MinecraftServer.getDimensionTypeManager().addDimension(DIMENSION_TYPE);

    final var instanceManager = MinecraftServer.getInstanceManager();
    final var instanceContainer = instanceManager.createInstanceContainer(DIMENSION_TYPE);
    instanceContainer.setChunkGenerator(new EmptyChunkGenerator());

    final var games = Maps.<String, Parkour>newConcurrentMap();

    final var eventHandler = MinecraftServer.getGlobalEventHandler();

    eventHandler.addListener(
        PlayerLoginEvent.class,
        event -> {
          event.setSpawningInstance(instanceContainer);
        });

    eventHandler.addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));

    eventHandler.addListener(
        PlayerMoveEvent.class,
        event -> {
          final var player = event.getPlayer();
          final var landingPosition = event.getNewPosition().withY(y -> y - 1);
          final var point =
              new Pos(landingPosition.blockX(), landingPosition.blockY(), landingPosition.blockZ());
          final var game = games.get(player.getUsername());
          event.setNewPosition(game.handleMoveToPoint(event.getNewPosition(), point));
        });

    final var tasks = Maps.<String, Task>newConcurrentMap();

    eventHandler.addListener(
        PlayerSpawnEvent.class,
        event -> {
          final var player = event.getPlayer();
          //      final var changeGameStatePacket = new ChangeGameStatePacket();
          //      changeGameStatePacket.reason = Reason.BEGIN_RAINING;
          //      event.getPlayer().sendPacket(changeGameStatePacket);
          games.put(player.getUsername(), new Parkour(player, ThreadLocalRandom.current()));
          player.teleport(new Pos(0.5, 61, 0.5));
          player.setLevel(-999);
          player.setGameMode(GameMode.ADVENTURE);

          final var task =
              MinecraftServer.getSchedulerManager()
                  .buildTask(
                      () -> {
                        if (!player.isOnline()) {
                          return;
                        }
                        player.sendPacket(
                            ParticleCreator.createParticlePacket(
                                Particle.END_ROD,
                                true,
                                player.getPosition().x(),
                                player.getPosition().y(),
                                player.getPosition().z(),
                                20,
                                20,
                                20,
                                0.001f,
                                500,
                                null));
                      })
                  .repeat(Duration.ofSeconds(2))
                  .schedule();

          tasks.put(player.getUsername(), task);
        });

    eventHandler.addListener(
        PlayerDisconnectEvent.class,
        event -> {
          games.remove(event.getPlayer().getUsername());
          Optional.ofNullable(tasks.remove(event.getPlayer().getUsername()))
              .ifPresent(Task::cancel);
        });

    minecraftServer.start("0.0.0.0", 25565);

    instanceContainer.enableAutoChunkLoad(true);
    // instanceContainer.setTime(18000);
    instanceContainer.setTime(6000);
    instanceContainer.setTimeRate(0);
    instanceContainer.setBlock(0, 60, 0, Block.BEACON);
  }

  private static class EmptyChunkGenerator implements ChunkGenerator {

    @Override
    public void generateChunkData(ChunkBatch batch, int chunkX, int chunkZ) {}

    @Override
    public List<ChunkPopulator> getPopulators() {
      return List.of(
          (batch, chunk) -> {
            for (int x = 0; x < 16; x++) {
              for (int y = 0; y < 128; y++) {
                for (int z = 0; z < 16; z++) {
                  chunk.setBiome(x, y, z, BIOME);
                }
              }
            }
          });
    }
  }
}
