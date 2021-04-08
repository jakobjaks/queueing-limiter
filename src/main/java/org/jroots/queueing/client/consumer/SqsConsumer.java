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

public class SqsConsumer implements QueueConsumer {

    private final HandlerService handlerService;
    private final String sqsUrl;
    private final AmazonSQS amazonSQSClient;

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter requests = metrics.meter("requests");

    private final Logger logger = LoggerFactory.getLogger(SqsConsumer.class);

    public SqsConsumer(HandlerService handlerService, QueueLimiterConfiguration configuration) {
        this.handlerService = handlerService;
        sqsUrl = configuration.getInboundSqsUrl();
        amazonSQSClient = AmazonSQSClientBuilder.standard().withRegion("us-east-1").build();
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();
    }

    @Override
    public void startConsuming() {
        logger.info("===STARTED CONSUMING===");
        while (true) {
            try {
                var request = new ReceiveMessageRequest().withWaitTimeSeconds(20).withQueueUrl(sqsUrl);
                var messages = amazonSQSClient.receiveMessage(request).getMessages();

                for (var message : messages) {
                    var internalMessage = convertToInternalMessage(message);
                    requests.mark();
                    handlerService.handlePayload(internalMessage)
                            .thenAccept(timeLeft -> {
                                deleteMessage(message.getReceiptHandle());
                                if (timeLeft > 60) {
                                    resendMessage(internalMessage, timeLeft);
                                }
                            });

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
