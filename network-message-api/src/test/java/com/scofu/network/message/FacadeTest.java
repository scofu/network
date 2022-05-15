package com.scofu.network.message;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import com.scofu.network.message.facade.Facade;
import com.scofu.network.message.facade.annotation.Subscribe;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Tests the {@link com.scofu.network.message.facade.Facade}. */
public class FacadeTest extends Service {

  @Inject private Facade facade;

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bind(Dispatcher.class).to(LocalDispatcher.class).in(Scopes.SINGLETON);
  }

  @Test
  public void test() {
    load(Stage.PRODUCTION, this);
    final var pong = facade.proxy(Pinger.class).publish(new Ping(System.nanoTime(), "hi")).join();
    assertNotNull(pong);
    assertEquals("hello", pong.tag());
  }

  /** Facade for {@link Ping}. */
  public interface Pinger {

    /**
     * Publisher.
     *
     * @param ping the ping
     */
    CompletableFuture<Ping> publish(Ping ping);

    /**
     * Subscriber.
     *
     * @param ping the ping
     */
    @Subscribe
    default CompletableFuture<Ping> subscribe(Ping ping) {
      return completedFuture(ping.withTag("hello"));
    }
  }
}
