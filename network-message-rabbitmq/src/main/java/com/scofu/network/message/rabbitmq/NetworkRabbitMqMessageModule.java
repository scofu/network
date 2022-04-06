package com.scofu.network.message.rabbitmq;

import com.google.inject.Scopes;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;
import com.scofu.network.message.Dispatcher;

/**
 * Network rabbitmq message module.
 */
@Module
public class NetworkRabbitMqMessageModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bind(Dispatcher.class).to(RabbitMqDispatcher.class).in(Scopes.SINGLETON);
  }
}
