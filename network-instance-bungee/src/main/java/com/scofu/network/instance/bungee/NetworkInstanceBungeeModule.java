package com.scofu.network.instance.bungee;

import com.google.inject.Scopes;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;

/** Network instance bungee module. */
@Module
public class NetworkInstanceBungeeModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bindFeature(NetworkListener.class).in(Scopes.SINGLETON);
    bindFeature(InstanceListener.class).in(Scopes.SINGLETON);
    bindFeature(ResourcePackListener.class).in(Scopes.SINGLETON);
  }
}
