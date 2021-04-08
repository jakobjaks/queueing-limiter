package org.jroots.queueing.client.consumer;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.jroots.queueing.service.HandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RabbitConsumer implements QueueConsumer {

    private final static String QUEUE_NAME = "test-queue";
    private final Logger logger = LoggerFactory.getLogger(RabbitConsumer.class);

    private Connection connection;
    private Channel channel;

    private final ConnectionFactory factory;

    private final HandlerService handlerService;

    public RabbitConsumer(HandlerService handlerService) {
        factory = new ConnectionFactory();
        factory.setHost("docker.for.mac.localhost");

        this.handlerService = handlerService;
    }

    @Override
    public void startConsuming() {
        logger.info("Starting to consume from RabbitMQ");
        Connection connection = null;
        Channel channel = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            logger.info(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                var stringPayload = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info(" [x] Received '{}'", stringPayload);
                handlerService.handlePayload(null);
            };

            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            startConsuming();
        }
    }

    @Override
    public void stopConsuming() {
        logger.warn("Aborting Consuming from RabbitMQ");
        try {
            if (channel != null) {
                channel.abort();
            }
            if (connection != null) {
                connection.abort();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
