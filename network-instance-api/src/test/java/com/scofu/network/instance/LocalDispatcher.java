package com.scofu.network.instance;

import com.google.inject.Inject;
import com.scofu.network.message.Dispatcher;
import com.scofu.network.message.MessageFlow;
import java.io.IOException;

final class LocalDispatcher implements Dispatcher {

  private final MessageFlow messageFlow;

  @Inject
  LocalDispatcher(MessageFlow messageFlow) {
    this.messageFlow = messageFlow;
  }

  @Override
  public void dispatchFanout(String topic, byte... message) {
    messageFlow
        .handleMessageOrRequest(message)
        .whenComplete(
            (reply, throwable) -> {
              if (throwable != null) {
                throwable.printStackTrace();
                return;
              }
              if (reply == null || reply.length == 0) {
                return;
              }
              dispatchFanout(topic, reply);
            });
  }

  @Override
  public void dispatchRequest(String topic, byte... message) {
    dispatchFanout(topic, message);
  }

  @Override
  public void close() throws IOException {}
}
