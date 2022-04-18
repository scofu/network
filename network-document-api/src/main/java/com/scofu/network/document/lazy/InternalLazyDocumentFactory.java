package com.scofu.network.document.lazy;

import static com.jsoniter.any.Any.rewrap;
import static com.jsoniter.any.Any.wrap;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Optional.empty;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.internal.MoreTypes;
import com.jsoniter.ValueType;
import com.jsoniter.annotation.JsonProperty;
import com.jsoniter.any.Any;
import com.jsoniter.spi.TypeLiteral;
import com.scofu.common.json.Json;
import com.scofu.network.document.lazy.InternalLazyDocumentFactory.Binding.Type;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class InternalLazyDocumentFactory implements LazyDocumentFactory {

  private final Map<Method, Optional<Binding>> bindings;
  private final Json json;

  @Inject
  InternalLazyDocumentFactory(Json json) {
    this.json = json;
    this.bindings = Maps.newConcurrentMap();
  }

  @Override
  public <T extends LazyDocument> T create(Class<T> type, String id, Any any) {
    final var document = newProxyInstance(getClass().getClassLoader(), new Class[]{type},
        new Body(any, id));
    any.asMap().put("_id", wrap(id));
    return type.cast(document);
  }

  @Override
  public <T extends LazyDocument> T create(Class<T> type, String id) {
    final var any = rewrap(Maps.newLinkedHashMap());
    any.asMap().put("_id", wrap(id));
    return create(type, id, any);
  }

  private Optional<Binding> parseBinding(Method method) {
    if (Void.TYPE.isAssignableFrom(method.getReturnType())) {
      if (method.getParameterCount() == 1) {
        final var key = parseKey(method, "setIs").or(() -> parseKey(method, "set"))
            .orElseGet(method::getName);
        return Optional.of(
            new Binding(key, TypeLiteral.create(method.getGenericParameterTypes()[0]),
                Type.SETTER));
      }
      return empty();
    }
    if (method.getParameterCount() != 0) {
      return empty();
    }
    final var key = parseKey(method, "get").or(() -> parseKey(method, "is"))
        .orElseGet(method::getName);
    if (Optional.class.isAssignableFrom(method.getReturnType())) {
      final var type = method.getGenericReturnType() instanceof ParameterizedType parameterizedType
          ? parameterizedType.getActualTypeArguments()[0] : method.getGenericReturnType();
      return Optional.of(new Binding(key, TypeLiteral.create(type), Type.OPTIONAL_GETTER));
    }
    return Optional.of(
        new Binding(key, TypeLiteral.create(method.getGenericReturnType()), Type.GETTER));
  }

  private Optional<String> parseKey(Method method, String prefix) {
    final var annotation = method.getAnnotation(JsonProperty.class);
    if (annotation != null) {
      return Optional.of(annotation.value());
    }
    // ...
    if (!method.getName().startsWith(prefix)) {
      return empty();
    }
    if (method.getName().length() == prefix.length() + 1) {
      // set<...>
      final var character = method.getName().substring(prefix.length()).charAt(0);
      if (!Character.isUpperCase(character)) {
        // seta
        // a
        return empty();
      }
      // setA
      // A
      // a
      return Optional.of(String.valueOf(Character.toLowerCase(character)));
    }
    // setAnimal
    // nimal
    final var substring = method.getName().substring(prefix.length() + 1);
    // a + nimal
    return Optional.of(Character.toLowerCase(method.getName().charAt(prefix.length())) + substring);
  }

  record Binding(String key, TypeLiteral<?> typeLiteral, Type type) {

    public Object read(Any any) {
      final var rawType = MoreTypes.getRawType(typeLiteral.getType());
      if (boolean.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.BOOLEAN && any.toBoolean();
      } else if (int.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.NUMBER ? any.toInt() : 0;
      } else if (long.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.NUMBER ? any.toLong() : 0L;
      } else if (float.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.NUMBER ? any.toFloat() : 0F;
      } else if (double.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.NUMBER ? any.toDouble() : 0D;
      } else if (BigInteger.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.NUMBER ? any.toBigInteger() : BigInteger.ZERO;
      } else if (BigDecimal.class.isAssignableFrom(rawType)) {
        return any.valueType() == ValueType.NUMBER ? any.toBigDecimal() : BigDecimal.ZERO;
      } else {
        return any.as(typeLiteral);
      }
    }

    enum Type {
      GETTER,
      OPTIONAL_GETTER,
      SETTER
    }
  }

  final class Body implements InvocationHandler, LazyDocument {

    private final Any any;
    private final String id;

    private Body(Any any, String id) {
      this.any = any;
      this.id = id;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.isDefault()) {
        return InvocationHandler.invokeDefault(proxy, method, args);
      } else if (method.getDeclaringClass().isAssignableFrom(getClass())) {
        return method.invoke(this, args);
      } else {
        return bindings.computeIfAbsent(method, InternalLazyDocumentFactory.this::parseBinding)
            .flatMap(binding -> invokeBinding(binding, args))
            .orElse(null);
      }
    }

    @Override
    public Any any() {
      return any;
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public int hashCode() {
      return any.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this || obj instanceof LazyDocument lazyDocument && Objects.equals(id,
          lazyDocument.id());
    }

    @Override
    public String toString() {
      return json.toString(Any.class, any);
    }

    private Optional<?> invokeBinding(Binding binding, Object[] args) {
      switch (binding.type) {
        case GETTER -> {
          return Optional.ofNullable(any.asMap().getOrDefault(binding.key, Any.wrapNull()))
              .map(binding::read);
        }
        case OPTIONAL_GETTER -> {
          return Optional.of(
              Optional.ofNullable(any.asMap().getOrDefault(binding.key, Any.wrapNull()))
                  .map(binding::read));
        }
        case SETTER -> {
          if (args[0] == null) {
            any.asMap().remove(binding.key);
            return Optional.empty();
          }
          any.asMap().put(binding.key, new ForwardingAny(args[0], binding.typeLiteral));
          return Optional.empty();
        }
        default -> throw new UnsupportedOperationException();
      }
    }
  }
}
