package com.scofu.network.message.internal;

import com.google.inject.TypeLiteral;

final class Types {

  private static final TypeLiteral<Void> VOID_TYPE_LITERAL = TypeLiteral.get(Void.TYPE);

  private Types() {
  }

  public static TypeLiteral<Void> voidTypeLiteral() {
    return VOID_TYPE_LITERAL;
  }

}
