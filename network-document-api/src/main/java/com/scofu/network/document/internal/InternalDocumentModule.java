package com.scofu.network.document.internal;

import com.google.inject.Scopes;
import com.scofu.common.inject.AbstractFeatureModule;

/**
 * Internal document module.
 */
public class InternalDocumentModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bindFeature(FilterAdapter.class).in(Scopes.SINGLETON);
  }
}
