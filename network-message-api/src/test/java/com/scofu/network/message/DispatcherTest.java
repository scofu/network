package com.scofu.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Tests the {@link Dispatcher}. */
public class DispatcherTest extends Service {

  @Inject private MessageQueue messageQueue;
  @Inject private MessageFlow messageFlow;

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bind(Dispatcher.class).to(LocalDispatcher.class).in(Scopes.SINGLETON);
  }

  @Test
  public void test() {
    load(Stage.PRODUCTION, this);
    messageFlow
        .subscribeTo(Ping.class)
        .replyWith(Ping.class)
        .via(CompletableFuture::completedFuture);
    final var pong =
        messageQueue
            .declareFor(Ping.class)
            .expectReply(Ping.class)
            .push(new Ping(System.nanoTime(), "hi"))
            .thenApply(ping -> new Ping(System.nanoTime() - ping.ns(), "hello"))
            .join();
    assertNotNull(pong);
    assertEquals("hello", pong.tag());
  }
}
