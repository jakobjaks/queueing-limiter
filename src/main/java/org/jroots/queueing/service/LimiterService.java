package org.jroots.queueing.service;

import io.dropwizard.lifecycle.Managed;
import org.jroots.queueing.client.consumer.QueueConsumer;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class LimiterService implements Managed {

    private QueueConsumer queueConsumer;
    private CacheService cacheService;
    private final Executor executor;

    public LimiterService(QueueConsumer queueConsumer, CacheService cacheService, Executor executor) {
        this.queueConsumer = queueConsumer;
        this.cacheService = cacheService;
        this.executor = executor;
    }

    @Override
    public void start() throws Exception {
        queueConsumer.startConsuming();
    }

    @Override
    public void stop() throws Exception {
        queueConsumer.stopConsuming();
    }
}
