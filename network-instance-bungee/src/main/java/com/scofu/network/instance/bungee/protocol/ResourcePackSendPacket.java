package com.scofu.network.instance.bungee.protocol;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

/** Resource pack send packet. */
public class ResourcePackSendPacket extends DefinedPacket {

  private String url;
  private String hash;
  private boolean required;
  private String componentJson;

  /** Constructs a new resource pack send packet. */
  public ResourcePackSendPacket() {}

  /**
   * Constructs a new resource pack send packet.
   *
   * @param url the url
   * @param hash the hash
   * @param required if required
   * @param componentJson the component json
   */
  public ResourcePackSendPacket(String url, String hash, boolean required, String componentJson) {
    this.url = url;
    this.hash = hash;
    this.required = required;
    this.componentJson = componentJson;
  }

  @Override
  public void handle(AbstractPacketHandler handler) throws Exception {
    System.out.println("Handler: " + handler);
  }

  @Override
  public void write(ByteBuf buf) {
    writeString(url, buf);
    writeString(hash, buf);
    buf.writeBoolean(required);
    if (componentJson != null) {
      buf.writeBoolean(true);
      writeString(componentJson, buf);
    } else {
      buf.writeBoolean(false);
    }
  }

  @Override
  public void read(ByteBuf buf) {}

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourcePackSendPacket that = (ResourcePackSendPacket) o;
    return required == that.required
        && url.equals(that.url)
        && hash.equals(that.hash)
        && Objects.equals(componentJson, that.componentJson);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, hash, required, componentJson);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("url", url)
        .add("hash", hash)
        .add("required", required)
        .add("componentJson", componentJson)
        .toString();
  }
}
