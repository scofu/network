package com.scofu.network.document.lazy;

import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

final class ForwardingAny extends Any {

  private final Object object;
  private final TypeLiteral<?> typeLiteral;

  public ForwardingAny(Object object, TypeLiteral<?> typeLiteral) {
    this.object = object;
    this.typeLiteral = typeLiteral;
  }

  @Override
  public ValueType valueType() {
    if (object instanceof Number) {
      return ValueType.NUMBER;
    } else if (object instanceof Boolean) {
      return ValueType.BOOLEAN;
    } else if (object instanceof String) {
      return ValueType.STRING;
    } else if (object == null) {
      return ValueType.NULL;
    } else if (object.getClass().isArray()) {
      return ValueType.ARRAY;
    } else {
      return ValueType.OBJECT;
    }
  }

  @Override
  public Object object() {
    return object;
  }

  @Override
  public boolean toBoolean() {
    return (boolean) object;
  }

  @Override
  public int toInt() {
    return (int) object;
  }

  @Override
  public long toLong() {
    return (long) object;
  }

  @Override
  public float toFloat() {
    return (float) object;
  }

  @Override
  public double toDouble() {
    return (double) object;
  }

  @Override
  public BigInteger toBigInteger() {
    return (BigInteger) object;
  }

  @Override
  public BigDecimal toBigDecimal() {
    return (BigDecimal) object;
  }

  @Override
  public String toString() {
    return object.toString();
  }

  @Override
  public void writeTo(JsonStream jsonStream) throws IOException {
    jsonStream.writeVal(typeLiteral == null ? null : typeLiteral.getType(), object);
  }
}
