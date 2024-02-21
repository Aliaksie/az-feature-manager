package com.bootstrap.feature.management.util;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.FeatureLookup.FeatureManagementEnvironmentType;

/**
 * Feature proxy utils class.
 */
public final class FeatureProxyUtils {

    /**
     * Create async builder options for lookup operation.<br/>
     * Sets {@code defaultValue} to {@code true}.
     *
     * @param featureName toggle feature name
     * @param id user id
     * @param context context
     * @return {@code FeatureLookup.FeatureOptionsBuilderProvider}
     */
    public static FeatureLookup.FeatureOptionsBuilderProvider asyncFeatureOptions(String featureName, String id,
            ContextView context) {
        return asyncFeatureOptions(featureName, id, true, Set.of(), context);
    }

    /**
     * Create async builder options for lookup operation.
     *
     * @param featureName toggle feature name
     * @param id user id
     * @param defaultValue default value
     * @param groups groups
     * @param context context
     * @return {@code FeatureLookup.FeatureOptionsBuilderProvider}
     */
    public static FeatureLookup.FeatureOptionsBuilderProvider asyncFeatureOptions(String featureName, String id,
            boolean defaultValue, Set<String> groups, ContextView context) {
        return featureOptions(FeatureManagementEnvironmentType.WEB_REACTIVE, context::stream,
                featureName, id, defaultValue, groups);
    }

    /**
     * Create builder options for lookup operation.<br/>
     * Sets {@code defaultValue} to {@code true}.
     *
     * @param featureName toggle feature name
     * @param userId user id
     * @return {@code FeatureLookup.FeatureOptionsBuilderProvider}
     */
    public static FeatureLookup.FeatureOptionsBuilderProvider featureOptions(String featureName, String userId) {
        return featureOptions(featureName, userId, true, Set.of());
    }

    /**
     * Create builder options for lookup operation.
     *
     * @param featureName toggle feature name
     * @param userId user id
     * @param defaultValue default value
     * @param groups groups
     * @return {@code FeatureLookup.FeatureOptionsBuilderProvider}
     */
    public static FeatureLookup.FeatureOptionsBuilderProvider featureOptions(String featureName, String userId,
            boolean defaultValue, Set<String> groups) {
        return featureOptions(FeatureManagementEnvironmentType.WEB_SERVLET_BLOCKING,
                Stream::empty, featureName, userId, defaultValue, groups);
    }

    private static FeatureLookup.FeatureOptionsBuilderProvider featureOptions(
            FeatureManagementEnvironmentType type,
            Supplier<Stream<Map.Entry<Object, Object>>> entries,
            String featureName,
            String userId,
            boolean defaultValue,
            Set<String> groups) {
        return builder -> builder
                .featureLookupKey(type.getFeatureLookupKey(entries))
                .name(featureName)
                .userId(userId)
                .defaultValue(defaultValue)
                .groups(groups);
    }

    /**
     * Async feature proxy resolver.
     *
     * @param featureLookup is a {@link FeatureLookup} implementation
     * @param featureName feature name
     * @param userId defines user id
     * @param defaultValue default value
     * @param groups groups
     * @param oldMethod old method to invoke
     * @param newMethod new method to invoke
     * @param <R> response type
     * @return result of invoked method
     */
    public static <R> Mono<R> featureProxyResolverAsync(FeatureLookup featureLookup, String featureName, String userId,
            boolean defaultValue,
            Set<String> groups, Supplier<Mono<R>> oldMethod, Supplier<Mono<R>> newMethod) {
        return Mono.just(userId)
                .transformDeferredContextual((id, ctx) -> Mono.zip(id, Mono.just(ctx)))
                .filterWhen(t -> {
                    final var options = asyncFeatureOptions(featureName, t.getT1(), defaultValue, groups, t.getT2());
                    return featureLookup.lookupAsync(options);
                })
                .flatMap(ignored -> newMethod.get())
                .switchIfEmpty(Mono.defer(oldMethod));
    }

    /**
     * Async feature proxy resolver.
     *
     * @param featureLookup is a {@link FeatureLookup} implementation
     * @param featureName feature name
     * @param userId defines user id
     * @param oldMethod old method to invoke
     * @param newMethod new method to invoke
     * @param <R> response type
     * @return result of invoked method
     */
    public static <R> Mono<R> featureProxyResolverAsync(FeatureLookup featureLookup, String featureName, String userId,
            Supplier<Mono<R>> oldMethod, Supplier<Mono<R>> newMethod) {
        return featureProxyResolverAsync(featureLookup, featureName, userId, true,
                Set.of(), oldMethod, newMethod);
    }

    /**
     * Async feature proxy resolver dealing with {@link Flux} instances.
     *
     * @param featureLookup is a {@link FeatureLookup} implementation
     * @param featureName feature name
     * @param userId defines user id
     * @param defaultValue default value
     * @param groups groups
     * @param oldMethod old method to invoke
     * @param newMethod new method to invoke
     * @param <R> response type
     * @param <S> is a returning type of the old method
     * @param <T> is a returning type of the new method
     * @return result of invoked method
     */
    public static <R, T extends Publisher<R>, S extends Publisher<R>> Flux<R> featureProxyResolverAsyncMany(
            FeatureLookup featureLookup, String featureName, String userId,
            boolean defaultValue, Set<String> groups, Supplier<T> oldMethod, Supplier<S> newMethod) {
        return Mono.just(userId)
                .transformDeferredContextual((id, ctx) -> Mono.zip(id, Mono.just(ctx)))
                .filterWhen(t -> {
                    final var options = asyncFeatureOptions(featureName, t.getT1(), defaultValue, groups, t.getT2());
                    return featureLookup.lookupAsync(options);
                })
                .flatMapMany(ignored -> newMethod.get())
                .switchIfEmpty(oldMethod.get());
    }

    /**
     * Async feature proxy resolver dealing with {@link Flux} instances.
     *
     * @param featureLookup is a {@link FeatureLookup} implementation
     * @param featureName feature name
     * @param userId defines user id
     * @param oldMethod old method to invoke
     * @param newMethod new method to invoke
     * @param <R> response type
     * @param <S> is a returning type of the old method
     * @param <T> is a returning type of the new method
     * @return result of invoked method
     */
    public static <R, T extends Publisher<R>, S extends Publisher<R>> Flux<R> featureProxyResolverAsyncMany(
            FeatureLookup featureLookup, String featureName, String userId,
            Supplier<T> oldMethod, Supplier<S> newMethod) {
        return featureProxyResolverAsyncMany(featureLookup, featureName, userId, true,
                Set.of(), oldMethod, newMethod);
    }

    /**
     * Feature resolver (not async).
     *
     * @param <R> response type
     * @param featureLookup is a {@link FeatureLookup} implementation
     * @param featureName feature name
     * @param userId defines user id
     * @param oldMethod old method to invoke
     * @param newMethod new method to invoke
     * @param defaultValue default value
     * @param groups groups
     * @return result of invoked method
     */
    public static <R> R featureProxyResolver(FeatureLookup featureLookup, String featureName, String userId,
            boolean defaultValue, Set<String> groups, Supplier<R> oldMethod, Supplier<R> newMethod) {
        final var options = featureOptions(featureName, userId, defaultValue, groups);
        final var isFeatureEnabled = featureLookup.lookup(options);

        return isFeatureEnabled
                ? newMethod.get()
                : oldMethod.get();
    }

    /**
     * Feature resolver (not async). Sets {@code defaultValue} to {@code true}.
     *
     * @param <R> response type
     * @param featureLookup is a {@link FeatureLookup} implementation
     * @param featureName feature name
     * @param userId defines user id
     * @param oldMethod old method to invoke
     * @param newMethod new method to invoke
     * @return result of invoked method
     */
    public static <R> R featureProxyResolver(FeatureLookup featureLookup, String featureName, String userId,
            Supplier<R> oldMethod, Supplier<R> newMethod) {
        return featureProxyResolver(featureLookup, featureName, userId, true, Set.of(), oldMethod, newMethod);
    }

    private FeatureProxyUtils() {
    }

}
