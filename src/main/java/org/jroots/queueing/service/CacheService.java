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

    private HazelcastInstance andmevõrk;
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
            andmevõrk = HazelcastClient.newHazelcastClient(clientConfig);

            map = andmevõrk.getMap("rate-limits"); //creates the map proxy

            buckets = Bucket4j.extension(Hazelcast.class).proxyManagerForMap(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public CompletableFuture<Long> võtaŽetoon(Message message, int piirang) {
        if (andmevõrk == null) {
            //Alusta ühendust Hazelcast andmevõrguga
            startConnection();
        }
        return CompletableFuture.supplyAsync(() -> {
            var identifier = message.getIdentifier();
            long jääkAeg;
            //Proovi kätte saada kliendi piirangute ämber
            var ämberO = buckets.getProxy(identifier);
            if (ämberO.isPresent()) {
                var ämber = ämberO.get();
                var piirangud = map.get(identifier).getConfiguration().getBandwidths();
                //Võrdle kliendi ämbri mahtu ja andmebaasist päritud sätestatud mahtu
                if (piirangud[0].getCapacity() != piirang) {
                    Bandwidth uusPiirang = Bandwidth.simple(piirang, Duration.ofSeconds(1));
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(uusPiirang)
                            .build();
                    //Uuenda kliendi ämbri mahtu
                    ämber.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS);
                }
                tokenCounter.inc();
                jääkAeg = TimeUnit.NANOSECONDS.toSeconds(ämber.estimateAbilityToConsume(1).getNanosToWaitForRefill());
                //Kontrolli, et jääkaeg oleks väiksem kui lubatud
                if (jääkAeg < 30) {
                    //Võtaž etoon ämbrist
                    ämber.consumeIgnoringRateLimits(1);
                }
            } else {
                //Loo uus piirangute ämber
                var bucket = Bucket4j.extension(Hazelcast.class).builder()
                        .addLimit(Bandwidth.simple(piirang, Duration.ofSeconds(1)))
                        .build(map, identifier, RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION);
                bucket.tryConsume(1);
                jääkAeg = TimeUnit.NANOSECONDS.toSeconds(bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill());
            }
            //Tagasta aeg järgmise žetooni jõudmiseni ämbrisse
            return jääkAeg;
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
