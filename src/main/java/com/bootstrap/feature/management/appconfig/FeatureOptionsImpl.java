package com.bootstrap.feature.management.appconfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import com.azure.spring.cloud.feature.manager.FeatureFilter;
import com.azure.spring.cloud.feature.manager.entities.FeatureFilterEvaluationContext;
import com.azure.spring.cloud.feature.manager.feature.filters.TargetingFilter;
import com.azure.spring.cloud.feature.manager.targeting.ITargetingContextAccessor;
import com.azure.spring.cloud.feature.manager.targeting.TargetingContext;
import com.azure.spring.cloud.feature.manager.targeting.TargetingEvaluationOptions;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import com.bootstrap.feature.management.FeatureLookup;

/**
 * Feature description/configuration type to construct its behavior/expected state.
 */
record FeatureOptionsImpl(
        String featureLookupKey,
        String name,
        String userId,
        Set<String> groups,
        boolean defaultValue,
        boolean suppressFailure
) implements FeatureLookup.FeatureOptions {

    /**
     * Feature lookup cache/context key to apply if specified.
     *
     * @return feature lookup cache/context key
     */
    public String getFeatureLookupKey() {
        return Optional.ofNullable(featureLookupKey)
                .orElse(CustomFeatureManager.DEFAULT_FEATURE_MANAGEMENT_KEY);
    }

    /**
     * Default feature evaluation state to apply.
     *
     * @return feature evaluation state
     */
    public boolean getDefaultValue() {
        return defaultValue;
    }

    public static FeatureOptionsImplBuilder builder() {
        return new FeatureOptionsImplBuilder();
    }

    @Override
    public FeatureLookup.FeatureOptionsBuilder<? extends FeatureLookup.FeatureOptions> toBuilder() {
        return builder().featureLookupKey(this.featureLookupKey).name(this.name).userId(this.userId).groups(this.groups).defaultValue(this.defaultValue).suppressFailure(this.suppressFailure);
    }

    public static final class FeatureOptionsImplBuilder
            implements FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> {

        private static final long serialVersionUID = 1L;
        private String featureLookupKey;

        private String name;

        private String userId;

        private Set<String> groups;

        private boolean defaultValue;

        private boolean suppressFailure;

        @Override
        public FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> featureLookupKey(String featureLookupKey) {
            this.featureLookupKey = featureLookupKey;
            return this;
        }

        @Override
        public FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> userId(String userId) {
            this.userId = userId;
            return this;
        }

        @Override
        public FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> groups(Set<String> groups) {
            this.groups = groups;
            return this;
        }

        @Override
        public FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public FeatureLookup.FeatureOptionsBuilder<FeatureOptionsImpl> suppressFailure(boolean suppressFailure) {
            this.suppressFailure = suppressFailure;
            return this;
        }

        /**
         * Converts serialized {@linkplain FeatureLookup.FeatureOptionsBuilder}{@literal <}TYPE{@literal >}
         * representation into string.
         *
         * @return string variation of app configuration feature management configuration object
         */
        @Override
        public String stringify() {
            return Base64.getEncoder().encodeToString(SerializationUtils.serialize(this));
        }

        /**
         * Constructs full {@linkplain FeatureLookup.FeatureOptionsBuilder}{@literal <}TYPE{@literal >} representation
         * from provided string.
         *
         * @param value string variation of app configuration feature management configuration object
         * @return {@linkplain FeatureContext} holder object derived from specified
         * {@linkplain FeatureLookup.FeatureOptionsBuilder}{@literal <}TYPE{@literal >}
         */
        @Override
        public FeatureContext fromString(String value) {
            return Optional.ofNullable(value)
                    .filter(StringUtils::hasText)
                    .map(Base64.getDecoder()::decode)
                    .map(SerializationUtils::deserialize)
                    .filter(FeatureContext.class::isInstance)
                    .map(FeatureContext.class::cast)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Failed to construct 'TargetingFilter' instance"));
        }


        @Override
        public FeatureOptionsImpl build() {
            return new FeatureOptionsImpl(featureLookupKey, name, userId, groups, defaultValue, suppressFailure);
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();
            stream.writeObject(featureLookupKey);
            stream.writeObject(name);
            stream.writeObject(userId);
            stream.writeObject(groups);
        }

        @SuppressWarnings("squid:S2388")
        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            String featureLookupKey = Optional.ofNullable(stream.readObject())
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(CustomFeatureManager.DEFAULT_FEATURE_MANAGEMENT_KEY);

            String name = Optional.ofNullable(stream.readObject())
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Name parameter is required to construct 'FeatureOptions' builder instance"));

            String userId = Optional.ofNullable(stream.readObject())
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(null);

            @SuppressWarnings("unchecked")
            Set<String> groups = Optional.ofNullable(stream.readObject())
                    .filter(Set.class::isInstance)
                    .map(it -> (Set<String>) it)
                    .map(Collections::unmodifiableSet)
                    .orElseGet(Collections::emptySet);

            featureLookupKey(featureLookupKey).name(name).userId(userId).groups(groups);
        }

        private Object readResolve() {
            Map<String, Supplier<FeatureFilter>> map = Map.of("Microsoft.Targeting",
                    () -> new TargetingFilter(new FeatureOptionsAwareTargetingContextAccessor(this), new TargetingEvaluationOptions().setIgnoreCase(true)));

            return new FeatureContext(featureLookupKey, name, userId, groups, defaultValue, map);
        }

    }

    record FeatureContext(
            String featureLookupKey,
            String name,
            String userId,
            Set<String> groups,
            boolean defaultValue,
            Map<String, Supplier<FeatureFilter>> filters
    ) implements FeatureLookup.FeatureEvaluationContext<FeatureFilterEvaluationContext> {

        private static final FeatureFilter NO_OP_FILTER = context -> false;


        @Override
        public boolean evaluate(Iterable<FeatureFilterEvaluationContext> contexts) {
            return Optional.ofNullable(contexts)
                    .stream()
                    .flatMap(it -> StreamSupport.stream(it.spliterator(), false))
                    .map(this::applyFilters)
                    .reduce(Boolean::logicalOr)
                    .orElse(Boolean.FALSE);
        }

        private boolean applyFilters(FeatureFilterEvaluationContext context) {
            FeatureFilter filter = Optional.ofNullable(context)
                    .map(it -> {
                        it.setFeatureName(name);
                        return it.getName();
                    })
                    .filter(StringUtils::hasText)
                    .map(filters::get)
                    .map(Supplier::get)
                    .orElse(NO_OP_FILTER);

            return filter.evaluate(context);
        }

    }

    /**
     * {@linkplain FeatureLookup.FeatureOptions} aware context accessor to construct target filter evaluation context.
     */
    static class FeatureOptionsAwareTargetingContextAccessor implements ITargetingContextAccessor {

        FeatureOptionsImpl.FeatureOptionsImplBuilder featureOptionsBuilder;

        public FeatureOptionsAwareTargetingContextAccessor(FeatureOptionsImplBuilder featureOptionsBuilder) {
            this.featureOptionsBuilder = featureOptionsBuilder;
        }

        public FeatureOptionsImplBuilder getFeatureOptionsBuilder() {
            return featureOptionsBuilder;
        }

        /**
         * Builds the Targeting Context for Feature Targeting evaluation.
         *
         * @return Mono{@literal <}TargetContext{@literal >} context holder
         */
        @Override
        public Mono<TargetingContext> getContextAsync() {
            FeatureOptionsImpl featureOptions = featureOptionsBuilder.build();
            if (isTargetingAvailable(featureOptions)) {
                TargetingContext context = new TargetingContext();

                context.setUserId(featureOptions.userId());
                context.setGroups(new ArrayList<>(featureOptions.groups()));

                return Mono.just(context);
            } else {
                return Mono.empty();
            }
        }

        /**
         * Checks is target based filter can be applied.
         *
         * @return status value denoted the presence of target filter in the evaluation chain.
         */
        private boolean isTargetingAvailable(FeatureLookup.FeatureOptions featureOptions) {
            return Optional.ofNullable(featureOptions)
                    .filter(it -> StringUtils.hasText(it.userId()) || !CollectionUtils.isEmpty(it.groups()))
                    .isPresent();
        }

    }

}
