package com.scofu.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Tests timeout. */
public class TimeoutTest extends Service {

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
        .via(
            ping ->
                Result.of(
                    () -> {
                      try {
                        TimeUnit.MINUTES.sleep(1);
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                      return ping;
                    }));
    final var pong =
        messageQueue
            .declareFor(Ping.class)
            .expectReply(Ping.class)
            .push(new Ping(System.nanoTime(), "hi"))
            .timeoutAfter(1, TimeUnit.SECONDS, () -> new Ping(0, "timed out"))
            .join();
    assertNotNull(pong);
    assertEquals("timed out", pong.tag());
  }
}
