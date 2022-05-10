package com.scofu.network.instance;

import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;

/**
 * Network instance module.
 */
@Module
public class NetworkInstanceModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bind(InstanceRepository.class).in(Scopes.SINGLETON);
    bind(NetworkRepository.class).in(Scopes.SINGLETON);
    bind(FinalEndpointResolver.class).in(Scopes.SINGLETON);
    final var endpointResolverMultibinder = Multibinder.newSetBinder(binder(),
        EndpointResolver.class);
    endpointResolverMultibinder.addBinding()
        .to(NetworkBasedEndpointResolver.class)
        .in(Scopes.SINGLETON);
    endpointResolverMultibinder.addBinding()
        .to(TestDynamicEndpointResolver.class)
        .in(Scopes.SINGLETON);
  }
}
