package org.jroots.queueing.client.database;

import com.amazon.dax.client.dynamodbv2.AmazonDaxClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import org.jroots.queueing.QueueLimiterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DynamoDbClient implements LimitsDatabaseClient {

    private final AmazonDynamoDB amazonDynamoDBClient;
    private QueueLimiterConfiguration configuration;

    private final Logger logger = LoggerFactory.getLogger(DynamoDbClient.class);

    public DynamoDbClient(QueueLimiterConfiguration configuration) {
        this.configuration = configuration;
        AmazonDaxClientBuilder dynamoClientBuilder = AmazonDaxClientBuilder.standard();
        dynamoClientBuilder.withEndpointConfiguration(configuration.getDaxUrl());
        dynamoClientBuilder.setRegion("us-east-1");
        amazonDynamoDBClient = dynamoClientBuilder.build();
    }

    @Override
    public CompletableFuture<Integer> getLimit(String identifier) {
        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(configuration.getLimitsTableName())
                .addKeyEntry("identifier", new AttributeValue(identifier));

        return CompletableFuture.supplyAsync(() -> amazonDynamoDBClient.getItem(getItemRequest))
                .whenComplete((item, error) -> {
                    if (error != null) {
                        logger.warn("DynamoDB get failed, error: {} message: {}", error.getClass().getName(),
                                error.getMessage());
                    } else {
                        logger.info("Get data from DynamoDB, GetItemResult: {}", item.getItem());
                    }
                }).thenApply(getItemResult -> handleDynamoDbResponse(getItemResult.getItem()))
                .exceptionally(throwable -> {
                    logger.error(throwable.getLocalizedMessage());
                    return null;
                });
    }

    private int handleDynamoDbResponse(Map<String, AttributeValue> item) {
        return Integer.parseInt(item.get("limit_per_minute").getN());
    }
}
