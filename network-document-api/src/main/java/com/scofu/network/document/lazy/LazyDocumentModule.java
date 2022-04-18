package com.scofu.network.document.lazy;

import com.google.inject.Scopes;
import com.scofu.common.inject.AbstractFeatureModule;

/**
 * Lazy document module.
 */
public class LazyDocumentModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bind(LazyDocumentFactory.class).to(InternalLazyDocumentFactory.class).in(Scopes.SINGLETON);
    bindFeature(LazyDocumentAdapter.class).in(Scopes.SINGLETON);
  }
}
