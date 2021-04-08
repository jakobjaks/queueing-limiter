package org.jroots.queueing.service;

import org.jroots.queueing.api.Message;
import org.jroots.queueing.client.database.LimitsDatabaseClient;
import org.jroots.queueing.client.producer.QueueProducer;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class HandlerService {

    private final CacheService cacheService;
    private final QueueProducer queueProducer;
    private final LimitsDatabaseClient limitsDatabaseClient;

    public HandlerService(CacheService cacheService, QueueProducer queueProducer, LimitsDatabaseClient limitsDatabaseClient) {
        this.cacheService = cacheService;
        this.queueProducer = queueProducer;
        this.limitsDatabaseClient = limitsDatabaseClient;
    }

    public CompletableFuture<Long> handlePayload(Message message) {
        return limitsDatabaseClient.getLimit(message.getIdentifier())
                .thenCompose(limit -> cacheService.consumeTokens(message, limit))
                .thenApply(timeLeft -> {
                    if (timeLeft < 60) {
                        queueProducer.sendMessage(message, timeLeft);
                    }
                    return timeLeft;
                });
    }
}
