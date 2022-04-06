package com.scofu.network.message;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;
import com.scofu.network.message.facade.Facade;
import com.scofu.network.message.internal.InternalMessageModule;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Network message module.
 */
@Module
public class NetworkMessageModule extends AbstractFeatureModule {

  public static final String PUBLISHER_EXECUTOR_NAME = "scofu:publisher-executor";

  @Override
  protected void configure() {
    final var executorBinder = OptionalBinder.newOptionalBinder(binder(),
        Key.get(ExecutorService.class, Names.named(PUBLISHER_EXECUTOR_NAME)));
    executorBinder.setDefault().toProvider(this::createDefaultExecutor).in(Scopes.SINGLETON);
    install(new InternalMessageModule());
    bind(Facade.class).in(Scopes.SINGLETON);
  }

  private ExecutorService createDefaultExecutor() {
    return Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("Message Publisher (%d)").build());
  }
}
