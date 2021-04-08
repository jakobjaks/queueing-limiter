package org.jroots.queueing.client.producer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.jroots.queueing.QueueLimiterConfiguration;
import org.jroots.queueing.api.Message;

public class SqsProducer implements QueueProducer {

    private QueueLimiterConfiguration queueLimiterConfiguration;

    private final String sqsUrl;
    private final AmazonSQS amazonSQSClient;

    public SqsProducer(QueueLimiterConfiguration configuration) {
        sqsUrl = configuration.getOutboundSqsUrl();
        amazonSQSClient = AmazonSQSClientBuilder.standard().withRegion("us-east-1").build();
    }

    @Override
    public void sendMessage(Message message, long delay) {
        SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(sqsUrl)
                .withMessageBody(message.serializeToJson())
                .withDelaySeconds(Math.toIntExact(delay));
        amazonSQSClient.sendMessage(request);
    }
}
