package com.scofu.network.instance.service;

import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;

/**
 * Network instance service.
 */
public class NetworkInstanceService extends Service {

  public static void main(String[] args) {
    load(Stage.PRODUCTION, new NetworkInstanceService());
  }

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bindFeature(InstanceDeploymentController.class).in(Scopes.SINGLETON);
    bindFeature(InstanceAvailabilityController.class).in(Scopes.SINGLETON);
  }
}
