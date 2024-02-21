package com.bootstrap.feature.management.appconfig;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.cache.Cache;

/**
 * Facade type to express/implement specific app configuration feature management logic.
 */

public class FeatureLookupImpl implements FeatureLookup {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureLookupImpl.class);


    private static final String MANAGEMENT_NAMESPACE = MethodHandles.lookup().lookupClass().getName();

    private static final Scheduler DEFAULT_MANAGEMENT_SCHEDULER = Schedulers
            .newSingle(new FeatureLookupThreadFactory(MANAGEMENT_NAMESPACE));

    private final Cache<String, Serializable> cache;

    private final ApplicationContext context;

    private final boolean snapshotEnabled;

    public FeatureLookupImpl(Cache<String, Serializable> cache, ApplicationContext context, boolean snapshotEnabled) {
        this.cache = cache;
        this.context = context;
        this.snapshotEnabled = snapshotEnabled;
    }

    /**
     * Performs feature lookup operation based on specified {@linkplain FeatureOptions} configuration.
     *
     * @param options feature configuration object.
     * @return evaluated state of specified feature.
     */
    @Override
    public boolean lookup(FeatureOptions options) {
        FeatureOptions feature = FeatureOptionsImpl.builder()
                .featureLookupKey(options.featureLookupKey())
                .name(options.name())
                .groups(options.groups())
                .userId(options.userId())
                .defaultValue(options.defaultValue())
                .build();

        return lookupOperation(feature)
                .blockOptional()
                .orElseGet(feature::defaultValue);
    }

    /**
     * Performs feature lookup operation based on specified {@linkplain FeatureOptionsBuilderProvider} configurer.
     *
     * @param provider {@linkplain FeatureOptionsBuilderProvider} configurer.
     * @return evaluated state of specified feature.
     */
    @Override
    public boolean lookup(FeatureOptionsBuilderProvider provider) {
        FeatureOptions feature = provider.featureOptionsBuilder(FeatureOptionsImpl::builder).build();

        return lookupOperation(feature)
                .blockOptional()
                .orElseGet(feature::defaultValue);
    }

    /**
     * Non-blocking feature lookup operation based on specified {@linkplain FeatureOptionsBuilderProvider} configurer.
     *
     * @param provider {@linkplain FeatureOptionsBuilderProvider} configurer.
     * @return evaluated state of specified feature.
     */
    @Override
    public Mono<Boolean> lookupAsync(FeatureOptionsBuilderProvider provider) {
        FeatureOptions feature = provider.featureOptionsBuilder(FeatureOptionsImpl::builder).build();

        return lookupOperation(feature);
    }

    private Mono<Boolean> lookupOperation(FeatureOptions feature) {
        return Mono.defer(() -> getCustomFeatureManager(feature).isEnabledAsync(feature.toBuilder().stringify()))
                .subscribeOn(DEFAULT_MANAGEMENT_SCHEDULER)
                .doOnError(e -> LOG.error("Feature Lookup operation is failed. Reason - ", e))
                .onErrorResume(getFallbackPublisher(feature));
    }

    private CustomFeatureManager getCustomFeatureManager(FeatureOptions feature) {
        var customFeatureManagerBeanName = Optional.ofNullable(feature)
                .map(FeatureOptions::featureLookupKey)
                .filter(context::containsBeanDefinition)
                .orElse(CustomFeatureManager.DEFAULT_FEATURE_MANAGEMENT_KEY);

        return context.getBean(
                snapshotEnabled ? customFeatureManagerBeanName : CustomFeatureManager.DEFAULT_FEATURE_MANAGEMENT_KEY,
                CustomFeatureManager.class);
    }

    private Function<Throwable, Mono<Boolean>> getFallbackPublisher(FeatureOptions feature) {
        return cause -> Optional.ofNullable(feature)
                .filter(FeatureOptions::suppressFailure)
                .map(FeatureOptions::defaultValue)
                .map(Mono::just)
                .orElseGet(() -> Mono.error(() -> new FeatureLookupOperationException(cause)));
    }

    private static class FeatureLookupThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
        String name;

        AtomicLong counter = new AtomicLong();

        public FeatureLookupThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String newThreadName = String.join("-", name, Long.toString(counter.incrementAndGet()));
            Thread thread = new Thread(runnable, newThreadName);
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(this);
            return thread;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LOG.error("FeatureLookup worker failed with an uncaught exception%n", e);
        }

    }

    @SuppressWarnings("squid:S3985")
    private static class FeatureLookupOperationException extends RuntimeException {

        private final Throwable cause;


        public FeatureLookupOperationException(Throwable cause) {
            super(cause);
            this.cause = cause;
        }
    }

}
