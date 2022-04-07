package com.scofu.network.instance.bungee.protocol;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;

/**
 * Exposed protocol.
 */
public class ExposedProtocol {

  private static final Method MAPPING_FACTORY_METHOD;
  private static final Method REGISTER_PACKET_METHOD;
  private static final Class<?> PROTOCOL_MAPPING_CLASS;
  private static final Object TO_CLIENT_DIRECTION_DATA;

  static {
    try {
      MAPPING_FACTORY_METHOD = Protocol.class.getDeclaredMethod("map", int.class, int.class);
      MAPPING_FACTORY_METHOD.setAccessible(true);
      PROTOCOL_MAPPING_CLASS = MAPPING_FACTORY_METHOD.getReturnType();
      final var toClientField = Protocol.class.getDeclaredField("TO_CLIENT");
      toClientField.setAccessible(true);
      TO_CLIENT_DIRECTION_DATA = toClientField.get(Protocol.GAME);
      final var arrayClass = Array.newInstance(PROTOCOL_MAPPING_CLASS, 0).getClass();
      REGISTER_PACKET_METHOD = TO_CLIENT_DIRECTION_DATA.getClass()
          .getDeclaredMethod("registerPacket", Class.class, Supplier.class, arrayClass);
      REGISTER_PACKET_METHOD.setAccessible(true);
    } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Registers a to client packet.
   *
   * @param type     the type
   * @param supplier the supplier
   * @param id       the id
   * @param <T>      the type of the packet
   */
  public static <T extends DefinedPacket> void registerToClientPacket(Class<T> type,
      Supplier<? super T> supplier, int id) {
    final var array = (Object[]) Array.newInstance(PROTOCOL_MAPPING_CLASS, 1);
    try {
      array[0] = MAPPING_FACTORY_METHOD.invoke(null, ProtocolConstants.MINECRAFT_1_17, id);
      REGISTER_PACKET_METHOD.invoke(TO_CLIENT_DIRECTION_DATA, type, supplier, array);
    } catch (InvocationTargetException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}