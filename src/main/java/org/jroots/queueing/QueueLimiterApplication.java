package org.jroots.queueing;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jroots.queueing.configuration.ServicesConfiguration;
import org.jroots.queueing.health.TemplateHealthCheck;
import org.jroots.queueing.service.HandlerService;
import org.jroots.queueing.service.LimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class QueueLimiterApplication extends Application<QueueLimiterConfiguration> {

    private final Logger logger = LoggerFactory.getLogger(QueueLimiterApplication.class);

    public static void main(final String[] args) throws Exception {
        new QueueLimiterApplication().run(args);
    }

    @Override
    public String getName() {
        return "QueueLimiter";
    }

    @Override
    public void initialize(final Bootstrap<QueueLimiterConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                ));
    }

    @Override
    public void run(final QueueLimiterConfiguration configuration,
                    final Environment environment) {
        final TemplateHealthCheck healthCheck =
                new TemplateHealthCheck(configuration.getTemplate());
        environment.healthChecks().register("template", healthCheck);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        registerEnvironment(environment, ctx);
        registerConfiguration(configuration, ctx);

        ctx.refresh();

        var servicesConfiguration = new ServicesConfiguration(ctx);
        ctx.registerBean(ServicesConfiguration.class, servicesConfiguration);

    }

    private ConfigurableApplicationContext applicationContext() {
        var context = new AnnotationConfigApplicationContext();
        context.scan("org.jroots.queueing");
        return context;
    }

    private void registerEnvironment(Environment environment, ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton("appEnv", environment);
    }

    private void registerConfiguration(QueueLimiterConfiguration configuration, ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton("appConf", configuration);
    }
}
