package com.scofu.network.message.facade;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.MoreTypes;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.QueueBuilder;
import com.scofu.network.message.facade.annotation.Subscribe;
import com.scofu.network.message.facade.annotation.Topic;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Facade queues over interfaces by proxy. */
public class Facade {

  private final MessageQueue messageQueue;
  private final MessageFlow messageFlow;

  @Inject
  Facade(MessageQueue messageQueue, MessageFlow messageFlow) {
    this.messageQueue = messageQueue;
    this.messageFlow = messageFlow;
  }

  /**
   * Creates a new facade.
   *
   * @param type the type
   * @param <T> the type of the facade
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> T proxy(Class<T> type) {
    final var publishingMethods = Maps.<String, Function>newHashMap();
    final var instance = new AtomicReference<T>();
    for (var method : type.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Subscribe.class) && method.isDefault()) {
        registerSubscription(instance, method);
        continue;
      }
      registserPublisher(publishingMethods, method);
    }
    final var facade =
        type.cast(
            Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                new Class[] {type},
                (proxy, invokedMethod, args) -> {
                  if (invokedMethod.isDefault()) {
                    return MethodHandles.lookup()
                        .findSpecial(
                            type,
                            invokedMethod.getName(),
                            MethodType.methodType(
                                invokedMethod.getReturnType(), invokedMethod.getParameterTypes()),
                            type)
                        .bindTo(proxy)
                        .invokeWithArguments(args);
                  }
                  if (!invokedMethod.getDeclaringClass().equals(type)) {
                    throw new UnsupportedOperationException();
                  }
                  return publishingMethods.get(invokedMethod.getName()).apply(args[0]);
                }));
    instance.set(facade);
    return facade;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T> void registerSubscription(AtomicReference<T> instance, Method method) {
    Function function =
        o -> {
          try {
            return method.invoke(instance.get(), o);
          } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        };
    var topic =
        Optional.ofNullable(method.getAnnotation(Topic.class)).map(Topic::value).orElse(null);
    if (CompletableFuture.class.isAssignableFrom(
        MoreTypes.getRawType(method.getGenericReturnType()))) {
      final var builder =
          messageFlow
              .subscribeTo(TypeLiteral.get(method.getGenericParameterTypes()[0]))
              .replyWith(
                  TypeLiteral.get(
                      ((ParameterizedType) method.getGenericReturnType())
                          .getActualTypeArguments()[0]));
      if (topic != null) {
        builder.withTopic(topic).via(function);
      } else {
        builder.via(function);
      }
    } else if (Void.class.isAssignableFrom(MoreTypes.getRawType(method.getGenericReturnType()))) {
      final var builder =
          messageFlow.subscribeTo(TypeLiteral.get(method.getGenericParameterTypes()[0]));
      if (topic != null) {
        builder.withTopic(topic).via(function::apply);
      } else {
        builder.via(function::apply);
      }
    } else {
      final var builder =
          messageFlow
              .subscribeTo(TypeLiteral.get(method.getGenericParameterTypes()[0]))
              .replyWith(TypeLiteral.get(method.getGenericReturnType()));
      if (topic != null) {
        builder.withTopic(topic).via(function);
      } else {
        builder.via(function);
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void registserPublisher(Map<String, Function> publishingMethods, Method method) {
    QueueBuilder queue;
    if (CompletableFuture.class.isAssignableFrom(
        MoreTypes.getRawType(method.getGenericReturnType()))) {
      queue =
          messageQueue
              .declareFor(TypeLiteral.get(method.getGenericParameterTypes()[0]))
              .expectReply(
                  TypeLiteral.get(
                      ((ParameterizedType) method.getGenericReturnType())
                          .getActualTypeArguments()[0]));
    } else {
      queue = messageQueue.declareFor(TypeLiteral.get(method.getGenericParameterTypes()[0]));
    }
    if (method.isAnnotationPresent(Topic.class)) {
      queue = queue.withTopic(method.getAnnotation(Topic.class).value());
    }
    publishingMethods.put(method.getName(), queue::push);
  }
}
