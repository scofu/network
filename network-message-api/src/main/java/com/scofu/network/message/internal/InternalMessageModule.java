package com.scofu.network.message.internal;

import com.google.inject.Scopes;
import com.scofu.common.inject.AbstractFeatureModule;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import com.scofu.network.message.ObservableTopics;

/**
 * Internal message module.
 */
public class InternalMessageModule extends AbstractFeatureModule {

  @Override
  protected void configure() {
    bind(ObservableTopics.class).to(InternalObservableTopics.class).in(Scopes.SINGLETON);
    bind(MessageQueue.class).to(InternalMessageQueue.class).in(Scopes.SINGLETON);
    bind(MessageFlow.class).to(InternalMessageFlow.class).in(Scopes.SINGLETON);
    bind(PendingRequestStore.class).in(Scopes.SINGLETON);
  }
}
