package com.feature.management.appconfig;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Subscription;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import com.feature.management.FeatureManagerRegistrationCallback;
import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.appconfig.CustomFeatureManager;

/**
 * A {@link WebFilter} that manages lifecycle of synthetic {@linkplain CustomFeatureManager} bean definition instances.
 */

public class FeatureLookupWebFilter implements WebFilter, Ordered {

    private static final int FILTER_COMPONENT_ORDER = Ordered.LOWEST_PRECEDENCE - 1;

    private GenericApplicationContext context;

    private WebEndpointProperties webEndpointProperties;

    private Supplier<CustomFeatureManager> customFeatureManagerSnapshotSupplier;

    public FeatureLookupWebFilter(GenericApplicationContext context, WebEndpointProperties webEndpointProperties, Supplier<CustomFeatureManager> customFeatureManagerSnapshotSupplier) {
        this.context = context;
        this.webEndpointProperties = webEndpointProperties;
        this.customFeatureManagerSnapshotSupplier = customFeatureManagerSnapshotSupplier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<Void> source = chain.filter(exchange);
        return isActuatorEndpoint(getRequestPath(exchange)) ? source
                : new FeatureLookupWebFilter.MonoFeatureLookupWebFilter(source, context,
                customFeatureManagerSnapshotSupplier);
    }

    @Override
    public int getOrder() {
        return FILTER_COMPONENT_ORDER;
    }

    private boolean isActuatorEndpoint(String path) {
        var basePath = webEndpointProperties.getBasePath();
        var pathMapping = webEndpointProperties.getPathMapping();
        return Optional.ofNullable(path)
                .filter(it -> StringUtils.startsWithIgnoreCase(it, basePath) || pathMapping.containsKey(it))
                .isPresent();
    }

    private String getRequestPath(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange)
                .map(ServerWebExchange::getRequest)
                .map(ServerHttpRequest::getPath)
                .map(RequestPath::value)
                .orElseGet(getClass()::getName);
    }

    public GenericApplicationContext getContext() {
        return context;
    }

    public WebEndpointProperties getWebEndpointProperties() {
        return webEndpointProperties;
    }

    public Supplier<CustomFeatureManager> getCustomFeatureManagerSnapshotSupplier() {
        return customFeatureManagerSnapshotSupplier;
    }


    private static class MonoFeatureLookupWebFilter extends MonoOperator<Void, Void> implements
            FeatureManagerRegistrationCallback.ApplicationContextAwareFeatureManagerRegistrationCallback<CustomFeatureManager> {


        private final GenericApplicationContext context;

        private final Supplier<CustomFeatureManager> featureManagerCallback;

        volatile boolean useDefaultFeatureManager = false;

        MonoFeatureLookupWebFilter(
                Mono<? extends Void> source,
                GenericApplicationContext context,
                Supplier<CustomFeatureManager> featureManagerCallback) {
            super(source);
            this.context = context;
            this.featureManagerCallback = featureManagerCallback;
        }

        @Override
        public void subscribe(CoreSubscriber<? super Void> subscriber) {
            Context currentContext = subscriber.currentContext();
            source.subscribe(
                    new FeatureLookupWebFilter.MonoFeatureLookupWebFilter.FeatureLookupWebFilterSubscriber(subscriber,
                            currentContext,
                            this::registerFeatureManager,
                            this::unregisterFeatureManager, this::isUseDefaultFeatureManager));
        }

        public boolean isUseDefaultFeatureManager() {
            return useDefaultFeatureManager;
        }

        @Override
        public void useDefaultFeatureManagerCallback() {
            useDefaultFeatureManager = true;
        }

        @Override
        public Class<CustomFeatureManager> getFeatureManagerType() {
            return CustomFeatureManager.class;
        }

        @Override
        public GenericApplicationContext getContext() {
            return Optional.ofNullable(context)
                    .map(ApplicationContext::getParent)
                    .filter(GenericApplicationContext.class::isInstance)
                    .map(GenericApplicationContext.class::cast)
                    .orElse(context);
        }

        @Override
        public Supplier<CustomFeatureManager> getFeatureManagerCallback() {
            return featureManagerCallback;
        }

        static final class FeatureLookupWebFilterSubscriber extends BaseSubscriber<Void> {

            private final CoreSubscriber<? super Void> actual;

            private final Context context;

            private final Consumer<String> registerCallback;

            private final Consumer<String> unregisterCallback;

            private final Supplier<Boolean> useDefaultFeatureManager;

            FeatureLookupWebFilterSubscriber(
                    CoreSubscriber<? super Void> actual,
                    Context context,
                    Consumer<String> registerCallback,
                    Consumer<String> unregisterCallback,
                    Supplier<Boolean> useDefaultFeatureManager) {
                this.actual = actual;
                this.registerCallback = registerCallback;
                this.unregisterCallback = unregisterCallback;
                this.useDefaultFeatureManager = useDefaultFeatureManager;
                this.context = context.put(FeatureLookup.class.getName(),
                        FeatureManagerRegistrationCallback.getFeatureManagerKey(actual.hashCode()));
            }

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                actual.onSubscribe(this);
                if (upstream() != Operators.cancelledSubscription()) {
                    registerCallback.accept(getFeatureManagerName());
                }
            }

            @Override
            protected void hookOnNext(Void value) {
                if (!isDisposed()) {
                    actual.onNext(value);
                }
            }

            @Override
            protected void hookOnComplete() {
                actual.onComplete();
                unregisterCallback.accept(getFeatureManagerName());
            }

            @Override
            protected void hookOnCancel() {
                if (upstream() == Operators.cancelledSubscription()) {
                    unregisterCallback.accept(getFeatureManagerName());
                }
            }

            @Override
            protected void hookOnError(Throwable e) {
                actual.onError(e);
                unregisterCallback.accept(getFeatureManagerName());
            }

            @Override
            public Context currentContext() {
                boolean isDefaultFeatureManager = Optional.ofNullable(useDefaultFeatureManager)
                        .map(Supplier::get)
                        .orElse(Boolean.FALSE);

                return isDefaultFeatureManager ? context.delete(FeatureLookup.class.getName()) : context;
            }

            private String getFeatureManagerName() {
                return currentContext().get(FeatureLookup.class.getName());
            }

        }

    }

}
