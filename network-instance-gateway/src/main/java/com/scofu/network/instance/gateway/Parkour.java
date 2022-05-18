package com.scofu.network.instance.gateway;

import static com.scofu.text.ContextualizedComponent.success;
import static net.kyori.adventure.text.Component.text;

import com.scofu.text.Color;
import java.time.Duration;
import java.util.Random;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.FallingBlockMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;

final class Parkour {

  private static final Material[] MATERIALS =
      Material.values().stream()
          .filter(material -> isColoredMaterialName(material.name().toLowerCase()))
          .toArray(Material[]::new);
  private final Player player;
  private final Random random;
  private Point previousPoint;
  private Point nextPoint;
  private Point nextNextPoint;
  private int level;

  public Parkour(Player player, Random random) {
    this.player = player;
    this.random = random;
    this.nextPoint = generateNext(new Pos(0, 60, 0));
    this.nextNextPoint = generateNext(this.nextPoint);
    sendBlockChanges(true);
  }

  private static boolean isColoredMaterialName(String name) {
    if (name.contains("white")
        || name.contains("light_gray")
        || name.contains("gray")
        || name.contains("black")) {
      return false;
    }
    return name.contains("concrete")
        || name.contains("wool")
        || name.contains("terracotta")
        || (name.contains("stained") && !name.contains("pane"));
  }

  private static RgbColor colorByMaterialName(String name) {
    if (name.contains("red")) {
      return new RgbColor(222, 89, 89);
    }
    if (name.contains("brown")) {
      return new RgbColor(66, 51, 49);
    }
    if (name.contains("orange")) {
      return new RgbColor(230, 113, 46);
    }
    if (name.contains("yellow")) {
      return new RgbColor(230, 190, 46);
    }
    if (name.contains("lime")) {
      return new RgbColor(172, 230, 46);
    }
    if (name.contains("green")) {
      return new RgbColor(55, 156, 12);
    }
    if (name.contains("cyan")) {
      return new RgbColor(56, 214, 156);
    }
    if (name.contains("light_blue")) {
      return new RgbColor(56, 185, 214);
    }
    if (name.contains("blue")) {
      return new RgbColor(21, 86, 207);
    }
    if (name.contains("purple")) {
      return new RgbColor(102, 21, 207);
    }
    if (name.contains("magenta")) {
      return new RgbColor(163, 56, 224);
    }
    if (name.contains("pink")) {
      return new RgbColor(240, 120, 226);
    }
    return new RgbColor(255, 255, 255);
  }

  public Pos handleMoveToPoint(Pos position, Point point) {
    if (point.y() < 20) {
      if (previousPoint != null) {
        player.sendPacket(new BlockChangePacket(previousPoint, Block.VOID_AIR.stateId()));
      }
      player.sendPacket(new BlockChangePacket(nextPoint, Block.VOID_AIR.stateId()));
      player.sendPacket(new BlockChangePacket(nextNextPoint, Block.VOID_AIR.stateId()));
      previousPoint = null;
      nextPoint = generateNext(new Pos(0, 60, 0));
      nextNextPoint = generateNext(nextPoint);
      if (level > 0) {
        success().text("You reached level %s!", level).prefixed().render(player::sendMessage);
      }
      level = 0;
      MinecraftServer.getSchedulerManager()
          .buildTask(() -> sendBlockChanges(true))
          .delay(Duration.ofSeconds(1))
          .schedule();
      return new Pos(0.5, 61, 0.5);
    }
    if (isNextPoint(point, nextPoint)) {
      if (previousPoint != null) {
        player.sendPacket(new BlockChangePacket(previousPoint, Block.VOID_AIR.stateId()));
      }
      previousPoint = nextPoint;
      nextPoint = nextNextPoint;
      nextNextPoint = generateNext(nextNextPoint);
      level++;
      player.clearTitle();
      player.sendTitlePart(
          TitlePart.TIMES,
          Title.Times.of(Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(1)));
      player.sendActionBar(text(level).color(Color.BRIGHT_CYAN).decorate(TextDecoration.BOLD));
      sendBlockChanges(false);
      player.playSound(
          Sound.sound(
              Key.key("minecraft", "item.armor.equip_gold"),
              Source.MASTER,
              2f,
              random.nextInt(10, 21) * 0.1f),
          nextNextPoint.x(),
          nextNextPoint.y(),
          nextNextPoint.z());
    }
    return position;
  }

  private boolean isNextPoint(Point point, Point nextPoint) {
    if (point.equals(nextPoint)) {
      return true;
    }
    for (int x = (int) point.x() - 1; x <= point.x() + 1; x++) {
      for (int z = (int) point.z() - 1; z <= point.z() + 1; z++) {
        if (point.withX(x).withZ(z).equals(nextPoint)) {
          return true;
        }
      }
    }
    return false;
  }

  private void sendBlockChanges(boolean includeInitialPoint) {
    if (includeInitialPoint) {
      player.sendPacket(
          new BlockChangePacket(
              nextPoint, MATERIALS[random.nextInt(MATERIALS.length)].block().stateId()));
    }
    final var material = MATERIALS[random.nextInt(MATERIALS.length)];

    LivingEntity fallingBlock = new LivingEntity(EntityType.FALLING_BLOCK);
    FallingBlockMeta meta = (FallingBlockMeta) fallingBlock.getEntityMeta();
    meta.setHasNoGravity(false);
    meta.setBlock(material.block());
    meta.setInvisible(true);
    fallingBlock.setVelocity(new Vec(0, 20, 0));
    //    fallingBlock.addEffect(new Potion(PotionEffect.LEVITATION, (byte) 1, 60, false));
    fallingBlock.setInstance(
        player.getInstance(),
        nextNextPoint.withX(x -> x + 0.5).withY(y -> y - 5).withZ(z -> z + 0.5));
    fallingBlock.spawn();

    final var point = nextNextPoint;
    MinecraftServer.getSchedulerManager()
        .buildTask(
            () -> {
              fallingBlock.remove();
              player.sendPacket(new BlockChangePacket(point, material.block().stateId()));
            })
        .delay(Duration.ofMillis(250))
        .schedule();

    player.sendPacket(
        ParticleCreator.createParticlePacket(
            Particle.DUST_COLOR_TRANSITION,
            false,
            nextNextPoint.x() + 0.5,
            nextNextPoint.y() - 4.5,
            nextNextPoint.z() + 0.5,
            0,
            1.25f,
            0,
            0,
            100,
            writer -> {
              writer.writeFloat(0f);
              writer.writeFloat(0f);
              writer.writeFloat(0f);
              writer.writeFloat(4);
              final var color = colorByMaterialName(material.name().toLowerCase());
              writer.writeFloat(color.red);
              writer.writeFloat(color.green);
              writer.writeFloat(color.blue);
            }));
    player.sendPacket(
        ParticleCreator.createParticlePacket(
            Particle.DUST_COLOR_TRANSITION,
            false,
            nextNextPoint.x() + 0.5,
            nextNextPoint.y() - 0.5,
            nextNextPoint.z() + 0.5,
            0,
            0.125f,
            0,
            0,
            10,
            writer -> {
              writer.writeFloat(0f);
              writer.writeFloat(0f);
              writer.writeFloat(0f);
              writer.writeFloat(4);
              final var color = colorByMaterialName(material.name().toLowerCase());
              writer.writeFloat(color.red);
              writer.writeFloat(color.green);
              writer.writeFloat(color.blue);
            }));
  }

  private Point generateNext(Point point) {
    final var x = random.nextInt(-1, 2);
    final var y = random.nextInt(-1, 2);
    final var z = y <= 0 ? 4 : 3;
    return point.withX(i -> i + x).withY(i -> i + y).withZ(i -> i + z);
  }

  private record RgbColor(float red, float green, float blue) {

    public RgbColor(float red, float green, float blue) {
      this.red = red / 255;
      this.green = green / 255;
      this.blue = blue / 255;
    }
  }
}
