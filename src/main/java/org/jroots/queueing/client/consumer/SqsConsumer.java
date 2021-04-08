package org.jroots.queueing.client.consumer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import org.jroots.queueing.QueueLimiterConfiguration;
import org.jroots.queueing.api.Message;
import org.jroots.queueing.service.HandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SqsConsumer implements QueueConsumer {

    private final HandlerService handlerService;
    private final String sqsUrl;
    private final AmazonSQS amazonSQSClient;

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter requests = metrics.meter("requests");
    private final Executor executor;

    private final Logger logger = LoggerFactory.getLogger(SqsConsumer.class);

    public SqsConsumer(HandlerService handlerService, QueueLimiterConfiguration configuration, Executor executor) {
        this.handlerService = handlerService;
        this.executor = executor;
        sqsUrl = configuration.getInboundSqsUrl();
        amazonSQSClient = AmazonSQSClientBuilder.standard().withRegion("us-east-1").build();
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();
    }

    @Override
    public void startConsuming() {
        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            logger.info("===STARTED CONSUMING===");
            while (true) {
                try {
                    logger.info("In while loop for executor");
                    var request = new ReceiveMessageRequest().withWaitTimeSeconds(20).withQueueUrl(sqsUrl);
                    var messages = amazonSQSClient.receiveMessage(request).getMessages();

                    for (var message : messages) {
                        executor.execute(() -> {
                            logger.info("Started executor");
                            var internalMessage = convertToInternalMessage(message);
                            requests.mark();
                            handlerService.handlePayload(internalMessage)
                                    .thenAccept(timeLeft -> {
                                        logger.info("Deleting message with id {}", internalMessage.getUUID());
                                        deleteMessage(message.getReceiptHandle());
                                        if (timeLeft > 60) {
                                            logger.info("Resending message with id {}", internalMessage.getUUID());
                                            resendMessage(internalMessage, timeLeft);
                                        }
                                    });
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void deleteMessage(String receiptHandle) {
        amazonSQSClient.deleteMessage(sqsUrl, receiptHandle);
    }

    public void resendMessage(Message message, long visilibityTimeout) {
        SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(sqsUrl)
                .withMessageBody(message.serializeToJson())
                .withDelaySeconds(Math.toIntExact(visilibityTimeout));
        amazonSQSClient.sendMessage(request);
    }

    @Override
    public void stopConsuming() {

    }

    private Message convertToInternalMessage(com.amazonaws.services.sqs.model.Message message) {
        var internalMessage = new Message().deserializeFromJson(message.getBody());
        internalMessage.setReceiptHandle(message.getReceiptHandle());
        return internalMessage;
    }
}
