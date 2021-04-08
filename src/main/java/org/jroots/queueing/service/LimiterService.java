package org.jroots.queueing.service;

import io.dropwizard.lifecycle.Managed;
import org.jroots.queueing.client.consumer.QueueConsumer;
import org.springframework.stereotype.Service;

@Service
public class LimiterService implements Managed {

    private QueueConsumer queueConsumer;
    private CacheService cacheService;

    public LimiterService(QueueConsumer queueConsumer, CacheService cacheService) {
        this.queueConsumer = queueConsumer;
        this.cacheService = cacheService;
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
