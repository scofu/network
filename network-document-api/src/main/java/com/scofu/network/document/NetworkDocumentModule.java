package com.scofu.network.document;

import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.common.inject.annotation.Module;
import com.scofu.network.document.internal.InternalDocumentModule;
import com.scofu.network.document.lazy.LazyDocumentModule;

/**
 * Network document module.
 */
@Module
public class NetworkDocumentModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    install(new InternalDocumentModule());
    install(new LazyDocumentModule());
  }
}
