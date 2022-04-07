package com.scofu.network.instance;

import com.google.inject.Scopes;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;

/**
 * Network instance module.
 */
@Module
public class NetworkInstanceModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bindFeature(DomainAdapter.class).in(Scopes.SINGLETON);
    bind(NetworkRepository.class).in(Scopes.SINGLETON);
    bind(GroupRepository.class).in(Scopes.SINGLETON);
    //    bind(InstanceRepository.class).in(Scopes.SINGLETON);
  }
}
