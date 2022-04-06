package com.scofu.network.message.rabbitmq;

import com.google.inject.Inject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import com.scofu.network.message.Dispatcher;
import com.scofu.network.message.MessageFlow;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Basic dispatcher implementation for RabbitMQ.
 *
 * <p>Each dispatcher has its own "reply-to" queue. Messages are published through a global
 * exchange. Messages that expect a reply will set the "reply-to" property to its own "reply-to"
 * queue name. When a message is received it is processed through {@link
 * MessageFlow#handleMessageOrRequest(byte...)}. If a reply is returned from that method and the
 * received message had a "reply-to" property set it will be published in the empty exchange with
 * that "reply-to" property set as routing key.
 */
final class RabbitMqDispatcher implements Dispatcher {

  private static final AMQP.BasicProperties PROPERTIES;
  private static final String GLOBAL_EXCHANGE = System.getenv("RABBITMQ_EXCHANGE");

  static {
    PROPERTIES = new AMQP.BasicProperties.Builder().expiration("60000").build();
  }

  private final ConcurrentChannel concurrentChannel;
  private final AtomicReference<String> replyQueueReference = new AtomicReference<>();

  @Inject
  RabbitMqDispatcher(MessageFlow messageFlow) {
    this.concurrentChannel = ConcurrentChannel.of(createConnectionFactory());
    awaitChannelSetup(messageFlow);
  }

  @Override
  public void dispatchFanout(String topic, byte... message) {
    concurrentChannel.accept(
        channel -> channel.basicPublish(GLOBAL_EXCHANGE, topic, PROPERTIES, message));
  }

  @Override
  public void dispatchRequest(String topic, byte... message) {
    final var correlationId = UUID.randomUUID().toString();
    final var properties = PROPERTIES.builder()
        .correlationId(correlationId)
        .replyTo(replyQueueReference.get())
        .build();
    concurrentChannel.accept(
        channel -> channel.basicPublish(GLOBAL_EXCHANGE, topic, properties, message));
  }

  @Override
  public void close() throws IOException {
    final var latch = new CountDownLatch(1);
    concurrentChannel.accept(channel -> {
      concurrentChannel.connection().close(1000);
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    concurrentChannel.executorService().shutdownNow();
  }

  private ConnectionFactory createConnectionFactory() {
    final var factory = new ConnectionFactory();
    factory.setHost(System.getenv("RABBITMQ_HOST"));
    factory.setPort(Integer.parseInt(System.getenv("RABBITMQ_PORT")));
    factory.setUsername(System.getenv("RABBITMQ_USERNAME"));
    factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
    return factory;
  }

  private void awaitChannelSetup(MessageFlow messageFlow) {
    final var latch = new CountDownLatch(1);
    concurrentChannel.accept(channel -> {
      setupTopicObservationAndConsumer(messageFlow, channel);
      latch.countDown();
    });
    try {
      final var didTimeout = !latch.await(60, TimeUnit.SECONDS);
      if (didTimeout) {
        throw new IllegalStateException("Couldn't setup RabbitMQ channel.");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void setupTopicObservationAndConsumer(MessageFlow messageFlow, Channel channel)
      throws IOException {
    replyQueueReference.set(channel.queueDeclare().getQueue());
    final var replyQueueName = replyQueueReference.get();
    channel.basicQos(100);
    channel.exchangeDeclare(GLOBAL_EXCHANGE, "topic", false, true, null);

    messageFlow.topics().observe(topic -> {
      try {
        channel.queueBind(replyQueueName, GLOBAL_EXCHANGE, topic);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    channel.basicConsume(replyQueueName, true,
        (consumerTag, delivery) -> consumeDelivery(messageFlow, channel, delivery), consumerTag -> {
        });
  }

  private void consumeDelivery(MessageFlow messageFlow, Channel channel, Delivery delivery) {
    //    System.out.println("delivery.getEnvelope() = " + delivery.getEnvelope());
    //    System.out.println("delivery.getProperties() = " + delivery.getProperties());
    messageFlow.handleMessageOrRequest(delivery.getBody()).whenComplete((reply, throwable) -> {
      if (throwable != null) {
        throwable.printStackTrace();
        return;
      }
      if (reply == null || reply.length == 0 || delivery.getProperties().getReplyTo() == null) {
        return;
      }
      publishReply(channel, reply, delivery);
    });
  }

  private void publishReply(Channel channel, byte[] reply, Delivery delivery) {
    try {
      channel.basicPublish("", delivery.getProperties().getReplyTo(), PROPERTIES, reply);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
