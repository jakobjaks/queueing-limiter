package org.jroots.queueing.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
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
    private ProxyManager<String> buckets;

    private HazelcastInstance hazelcastInstance;
    private final QueueLimiterConfiguration configuration;
    private MetricRegistry metricRegistry;

    private final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private final Counter tokenCounter;

    public CacheService(QueueLimiterConfiguration configuration, MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        this.tokenCounter = metricRegistry.counter("token_counter");
    }

    private void startConnection() {
        try {
            var clientConfig = createClientConfig();
            hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

            map = hazelcastInstance.getMap("rate-limits"); //creates the map proxy

            buckets = Bucket4j.extension(Hazelcast.class).proxyManagerForMap(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public CompletableFuture<Long> consumeTokens(Message message, int limit) {
        if (hazelcastInstance == null) {
            startConnection();
        }
        return CompletableFuture.supplyAsync(() -> {
            var identifier = message.getIdentifier();
            long secondsLeft;
            if (identifier == null) {
                identifier = "test";
            }
            var bucketO = buckets.getProxy(identifier);
            if (bucketO.isPresent()) {
                var bucket = bucketO.get();
                var bandWidths = map.get(identifier).getConfiguration().getBandwidths();
                if (bandWidths[0].getCapacity() != limit) {
                    Bandwidth newLimit = Bandwidth.simple(limit, Duration.ofSeconds(1));
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(newLimit)
                            .build();
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                }
                var tokens = bucket.getAvailableTokens();
                tokenCounter.inc();

                logger.info("Getting existing bucket for identifier {}", message.getIdentifier());
                logger.info("Numbers of tokens before consuming {}", tokens);

                secondsLeft = TimeUnit.NANOSECONDS.toSeconds(bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill());
                if (secondsLeft < 60) {
                    bucket.consumeIgnoringRateLimits(1);
                }
                logger.info("Numbers of tokens after consuming {}", bucket.getAvailableTokens());
                logger.info("Time left to acquire new tokens {}", TimeUnit.NANOSECONDS.toSeconds(bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill()));
            } else {
                logger.info("Creating a new bucket");
                var bucket = Bucket4j.extension(Hazelcast.class).builder()
                        .addLimit(Bandwidth.simple(limit, Duration.ofSeconds(1)))
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
