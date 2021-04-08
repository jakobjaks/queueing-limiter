package org.jroots.queueing.service;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.grid.hazelcast.Hazelcast;
import org.jroots.queueing.QueueLimiterConfiguration;
import org.jroots.queueing.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CacheService {

    @Inject
    private IMap<String, GridBucketState> map;

    private Bucket bucket;
    private final QueueLimiterConfiguration configuration;

    private final Logger logger = LoggerFactory.getLogger(CacheService.class);

    public CacheService(QueueLimiterConfiguration configuration) {
        this.configuration = configuration;
        var clientConfig = createClientConfig();
        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

        map = client.getMap("rate-limits"); //creates the map proxy

        bucket = Bucket4j.extension(Hazelcast.class).builder()
                .addLimit(Bandwidth.simple(3, Duration.ofMinutes(1)))
                .build(map, "newlimit", RecoveryStrategy.RECONSTRUCT);

    }

    public CompletableFuture<Long> consumeTokens(Message message, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            var identifier = message.getIdentifier();
            long secondsLeft;
            if (identifier == null) {
                identifier = "test";
            }
            if (map.containsKey(identifier)) {
                logger.info("Getting existing bucket for identifier {}", message.getIdentifier());
                logger.info("Numbers of tokens before consuming {}", bucket.getAvailableTokens());
                secondsLeft = TimeUnit.NANOSECONDS.toSeconds(bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill());
                if (secondsLeft < 60) {
                    bucket.consumeIgnoringRateLimits(1);
                }
                logger.info("Numbers of tokens after consuming {}", bucket.getAvailableTokens());
                logger.info("Time left to acquire new tokens {}", TimeUnit.NANOSECONDS.toSeconds(bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill()));
            } else {
                logger.info("Creating a new bucket");
                bucket = Bucket4j.extension(Hazelcast.class).builder()
                        .addLimit(Bandwidth.simple(limit, Duration.ofMinutes(1)))
                        .build(map, identifier, RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION);
                bucket.tryConsume(1);
                secondsLeft = TimeUnit.NANOSECONDS.toSeconds(bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill());
            }
            return secondsLeft;
        });
    }




    public ClientConfig createClientConfig() {
        var hazelcastIp = configuration.getHazelcastClusterIp();
        logger.info("Connect to Hazelcast using IP: {}", hazelcastIp);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hazelcastIp, hazelcastIp + "5701");
        return clientConfig;
    }
}
