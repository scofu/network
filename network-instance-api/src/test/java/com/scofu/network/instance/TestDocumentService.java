package com.scofu.network.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import com.scofu.common.json.PeriodEscapedString;
import com.scofu.common.json.lazy.LazyFactory;
import com.scofu.network.message.Dispatcher;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Basic network document test. */
public class TestDocumentService extends Service {

  @Inject private NetworkRepository networkRepository;
  @Inject private LazyFactory lazyFactory;

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bind(Dispatcher.class).to(LocalDispatcher.class).in(Scopes.SINGLETON);
    bindFeature(TestDocumentController.class).in(Scopes.SINGLETON);
  }

  @Test
  public void test() {
    load(Stage.PRODUCTION, this);
    final var id = "test-id";
    final var endpointDomain = "test.scofu.com";
    final var image = "docker.scofu.com/test-image";

    networkRepository
        .update(
            lazyFactory.create(
                Network.class,
                Network::id,
                id,
                Network::deployments,
                Map.of(
                    new PeriodEscapedString(endpointDomain),
                    lazyFactory.create(Deployment.class, Deployment::image, image))))
        .join();
    final var network = networkRepository.findById(id).join().orElse(null);
    final var deployment = network.deployments().get(new PeriodEscapedString(endpointDomain));
    assertNotNull(network);
    assertNotNull(deployment);
    assertEquals(image, deployment.image());
  }
}
