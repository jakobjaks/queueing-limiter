package org.jroots.queueing.configuration;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.jroots.queueing.QueueLimiterConfiguration;
import org.jroots.queueing.client.consumer.QueueConsumer;
import org.jroots.queueing.client.consumer.SqsConsumer;
import org.jroots.queueing.client.database.DynamoDbClient;
import org.jroots.queueing.client.database.LimitsDatabaseClient;
import org.jroots.queueing.client.producer.SqsProducer;
import org.jroots.queueing.service.CacheService;
import org.jroots.queueing.service.HandlerService;
import org.jroots.queueing.service.LimiterService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

@Configuration
public class ServicesConfiguration {

    private final QueueLimiterConfiguration configuration;
    private final ApplicationContext applicationContext;
    private final Environment environment;

    private final LimiterService limiterService;

    public ServicesConfiguration(@Nonnull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.configuration = (QueueLimiterConfiguration) applicationContext.getBean("appConf");
        this.environment = (Environment) applicationContext.getBean("appEnv");
        this.limiterService = limiterService();
        environment.lifecycle().manage(limiterService);

    }

    @Bean
    LimiterService limiterService() {
        return new LimiterService(sqsConsumer(), cacheService(), threadPoolTaskExecutor());
    }

    @Bean
    CacheService cacheService() {
        return new CacheService(configuration, metricsRegistry());
    }

    @Bean
    QueueConsumer sqsConsumer() {
        return new SqsConsumer(handlerService(), configuration, threadPoolTaskExecutor());
    }

    @Bean
    HandlerService handlerService() {
        return new HandlerService(cacheService(), new SqsProducer(configuration), limitsDatabaseClient());
    }

    @Bean
    LimitsDatabaseClient limitsDatabaseClient() {
        return new DynamoDbClient(configuration);
    }



    @Bean
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setThreadNamePrefix("sqsExecutor");
        executor.initialize();
        return executor;
    }

    @Bean
    public MetricRegistry metricsRegistry() {
        MetricRegistry metrics = new MetricRegistry();
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(metrics));
        return metrics;
    }
}
