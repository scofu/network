package com.scofu.network.instance.bukkit;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.inject.Named;

/**
 * Network instance bukkit module.
 */
@Module
public class NetworkInstanceBukkitModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bind(LocalInstanceProvider.class).in(Scopes.SINGLETON);
    bindFeature(LocalInstanceAnnouncer.class).in(Scopes.SINGLETON);
    bindFeature(TestListener.class).in(Scopes.SINGLETON);
    bindFeature(PlayerCountListener.class).in(Scopes.SINGLETON);
    bindFeature(DeployCommand.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  @Named("LocalHost")
  private InetAddress localHost() {
    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
}
