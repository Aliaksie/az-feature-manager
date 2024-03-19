package com.bootstrap.feature.management;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import reactor.core.publisher.Mono;

/**
 * Facade interface to extend for any specific feature management sdk provider.
 */
public interface FeatureLookup {

    /**
     * Stands for basic (potentially blocking) feature lookup operation based on provided {@linkplain FeatureOptions}
     * configuration.
     *
     * @param options feature configuration object.
     * @return evaluated state of specified feature.
     */
    boolean lookup(FeatureOptions options);

    /**
     * Performs feature lookup operation based on specified {@linkplain FeatureOptionsBuilderProvider} configurer.
     *
     * @param provider {@linkplain FeatureOptionsBuilderProvider} configurer.
     * @return evaluated state of specified feature.
     */
    boolean lookup(FeatureOptionsBuilderProvider provider);

    /**
     * Non-blocking feature lookup operation variation. By default considered as unsupported operation.
     *
     * @param options feature configuration object.
     * @return evaluated state of specified feature.
     */
    default Mono<Boolean> lookupAsync(FeatureOptions options) {
        throw new UnsupportedOperationException();
    }

    /**
     * Non-blocking feature lookup operation based on specified {@linkplain FeatureOptionsBuilderProvider} configurer.
     *
     * @param provider {@linkplain FeatureOptionsBuilderProvider} configurer.
     * @return evaluated state of specified feature.
     */
    default Mono<Boolean> lookupAsync(FeatureOptionsBuilderProvider provider) {
        throw new UnsupportedOperationException();
    }


    record DefaultFeatureOptions(String featureLookupKey,
                                 String name,
                                 String userId,
                                 Set<String> groups,
                                 boolean defaultValue,
                                 boolean suppressFailure) implements FeatureOptions {


        /**
         * Default feature evaluation state to apply.
         *
         * @return feature evaluation state
         */
        public boolean getDefaultValue() {
            return defaultValue;
        }

        /**
         * Default feature builder.
         */
        public static class DefaultFeatureOptionsBuilder implements FeatureOptionsBuilder<DefaultFeatureOptions> {
            private String featureLookupKey;
            private String name;
            private String userId;
            private Set<String> groups;
            private boolean defaultValue;
            private boolean suppressFailure;

            @Override
            public FeatureOptionsBuilder<DefaultFeatureOptions> featureLookupKey(String featureLookupKey) {
                this.featureLookupKey = featureLookupKey;
                return this;
            }

            @Override
            public FeatureOptionsBuilder<DefaultFeatureOptions> name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public FeatureOptionsBuilder<DefaultFeatureOptions> userId(String userId) {
                this.userId = userId;
                return this;
            }

            @Override
            public FeatureOptionsBuilder<DefaultFeatureOptions> groups(Set<String> groups) {
                this.groups = groups;
                return this;
            }

            @Override
            public FeatureOptionsBuilder<DefaultFeatureOptions> defaultValue(boolean defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            @Override
            public FeatureOptionsBuilder<DefaultFeatureOptions> suppressFailure(boolean suppressFailure) {
                this.suppressFailure = suppressFailure;
                return this;
            }

            @Override
            public DefaultFeatureOptions build() {
                return new DefaultFeatureOptions(featureLookupKey, name, userId, groups, defaultValue, suppressFailure);
            }
        }

        public static DefaultFeatureOptionsBuilder builder() {
            return new DefaultFeatureOptionsBuilder();
        }

        @Override
        public FeatureOptionsBuilder<? extends FeatureOptions> toBuilder() {
            return builder().suppressFailure(this.suppressFailure).name(this.name).userId(this.userId).groups(this.groups).defaultValue(this.defaultValue).suppressFailure(this.suppressFailure);
        }
    }


    /**
     * Interface to extend/implement for expressing configuration of feature management specific provider.
     */
    interface FeatureOptions {

        /**
         * Stands for cache/context feature lookup key.
         *
         * @return feature lookup key
         */
        String featureLookupKey();

        /**
         * Stands for feature name/alias.
         *
         * @return string representation of feature name/alias
         */
        String name();

        /**
         * User specific attribute/property to use for user-centric filtering.
         *
         * @return string representation of user identifier
         */
        String userId();

        /**
         * Set of groups for which the user is an inherent member based on the presence of specific attributes.
         *
         * @return set of possible groups user belongs to
         */
        Set<String> groups();

        /**
         * Default feature evaluation state to apply.
         *
         * @return feature evaluation state
         */
        boolean defaultValue();

        /**
         * Stands for suppressing any sort of exceptions upon feature evaluation process. Given default value is
         * provided - it will apply it as fallback error evaluation state.
         *
         * @return is suppress any sort of exceptions
         */
        boolean suppressFailure();

        /**
         * Constructs {@linkplain FeatureOptionsBuilder} builder derived from this options object.
         *
         * @return {@linkplain FeatureOptionsBuilder} builder
         */
        @SuppressWarnings("squid:S1452")
        default FeatureOptionsBuilder<? extends FeatureOptions> toBuilder() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Interface to configure/construct {@linkplain FeatureOptionsBuilder} instance.
     */
    @SuppressWarnings("squid:S1452")
    interface FeatureOptionsBuilderProvider extends UnaryOperator<FeatureOptionsBuilder<? extends FeatureOptions>> {

        /**
         * Apply feature management configuration options to specific builder instance.
         *
         * @param featureOptionsBuilder {@linkplain FeatureOptionsBuilder} builder creator
         * @return {@linkplain FeatureOptionsBuilder} builder configured instance
         */
        @SuppressWarnings("squid:S1452")
        default FeatureOptionsBuilder<? extends FeatureOptions> featureOptionsBuilder(
                Supplier<FeatureOptionsBuilder<? extends FeatureOptions>> featureOptionsBuilder) {
            return apply(featureOptionsBuilder.get());
        }

    }

    /**
     * Basic {@linkplain FeatureOptions} interface builder.
     *
     * @param <T> {@linkplain FeatureOptions} specific type parameter
     */
    interface FeatureOptionsBuilder<T extends FeatureOptions> extends Serializable {

        /**
         * Specify feature lookup key if it is applicable.
         *
         * @param featureLookupKey feature cache/lookup lookup key
         * @return builder mutable self reference
         */
        FeatureOptionsBuilder<T> featureLookupKey(String featureLookupKey);

        /**
         * Specify feature name/alias to apply.
         *
         * @param name feature name/alias
         * @return builder mutable self reference
         */
        FeatureOptionsBuilder<T> name(String name);

        /**
         * Specify user specific identity attribute to apply.
         *
         * @param userId feature name/alias
         * @return builder mutable self reference
         */
        FeatureOptionsBuilder<T> userId(String userId);

        /**
         * Specify user membership`s group set to apply.
         *
         * @param groups feature name/alias
         * @return builder mutable self reference
         */
        FeatureOptionsBuilder<T> groups(Set<String> groups);

        /**
         * Specify default feature state to apply.
         *
         * @param defaultValue feature name/alias
         * @return builder mutable self reference
         */
        FeatureOptionsBuilder<T> defaultValue(boolean defaultValue);

        /**
         * Specify option to suppress any popped up exceptions.
         *
         * @param suppressFailure option to suppress exceptions
         * @return builder mutable self reference
         */
        FeatureOptionsBuilder<T> suppressFailure(boolean suppressFailure);

        /**
         * Converts serialized {@linkplain FeatureLookup.FeatureOptionsBuilder}{@literal <}T{@literal >} representation
         * into string.
         *
         * @return string variation of app configuration feature management configuration object
         */
        default String stringify() {
            throw new UnsupportedOperationException();
        }

        /**
         * Constructs full {@linkplain FeatureLookup.FeatureOptionsBuilder}{@literal <}T{@literal >} representation from
         * provided string.
         *
         * @param value string variation of app configuration feature management configuration object
         * @return {@linkplain FeatureEvaluationContext} holder object derived from specified
         * {@linkplain FeatureLookup.FeatureOptionsBuilder}{@literal <}T{@literal >}
         */
        @SuppressWarnings("squid:S1452")
        default FeatureEvaluationContext<?> fromString(String value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Instantiates/creates specific {@linkplain FeatureOptions} object.
         *
         * @return {@linkplain FeatureOptions}
         */
        T build();

    }

    /**
     * Feature context interface to extend for custom filter evaluation logic.
     *
     * @param <T> filter evaluation context type if applicable
     */
    interface FeatureEvaluationContext<T> {

        /**
         * Stands for cache/context feature lookup key.
         *
         * @return feature lookup key
         */
        String featureLookupKey();

        /**
         * Stands for specific feature name/alias.
         *
         * @return name of the evaluated feature.
         */
        String name();

        /**
         * Validate evaluation filter contexts for specific feature configuration.
         *
         * @param contexts chain of the feature specific filters
         * @return state of evaluated filters chain
         */
        boolean evaluate(Iterable<T> contexts);

    }

    /**
     * List of possibly supported feature management providers.
     */
    enum FeatureManagerType {

        APP_CONFIGURATION(
                "com.azure.spring.cloud.feature.manager.FeatureManager"), LOCAL_CONFIGURATION, DARKLY, SPLIT, UNSPECIFIED;

        private final String type;

        private final boolean supported;

        FeatureManagerType() {
            this(null, false);
        }

        FeatureManagerType(String type) {
            this.type = type;
            this.supported = isTypeSupported(getClass().getClassLoader());
        }

        FeatureManagerType(String type, boolean supported) {
            this.type = type;
            this.supported = isTypeSupported(getClass().getClassLoader());
        }

        public boolean isSupported() {
            return supported;
        }

        private boolean isTypeSupported(ClassLoader classLoader) {
            if (classLoader == null) {
                classLoader = ClassUtils.getDefaultClassLoader();
            }
            try {
                var loader = Optional.ofNullable(classLoader)
                        .orElseGet(ClassUtils::getDefaultClassLoader);

                return StringUtils.hasText(type) && resolve(type, loader);
            } catch (Exception ex) {
                return false;
            }
        }

        /**
         * Suppress class loading warning as it is used and accessible only in private scope.
         *
         * @param className   type to verify
         * @param classLoader application classloader
         * @return is specified type presented on the classpath
         */
        @SuppressWarnings("squid:S2658")
        private static boolean resolve(String className, ClassLoader classLoader) {
            try {
                Class<?> type = (classLoader != null) ? Class.forName(className, false, classLoader) : Class.forName(className);
                return type.getName().equals(className);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

    }

    /**
     * Currently supported list of web-based environments by Feature Management Lookup.
     */
    enum FeatureManagementEnvironmentType {

        WEB_REACTIVE, WEB_SERVLET_BLOCKING {
            @Override
            protected String getDefaultFeatureLookupKey() {
                return (String) RequestContextHolder.currentRequestAttributes()
                        .getAttribute(FeatureLookup.class.getName(), RequestAttributes.SCOPE_REQUEST);
            }

        };

        protected String getDefaultFeatureLookupKey() {
            return null;
        }

        /**
         * Detects feature lookup key to leverage for lookup process.
         *
         * @param entries environment context entries
         * @return feature lookup key
         */
        public final String getFeatureLookupKey(Supplier<Stream<Map.Entry<Object, Object>>> entries) {
            return Optional.ofNullable(entries)
                    .map(Supplier::get)
                    .stream()
                    .flatMap(Function.identity())
                    .filter(Objects::nonNull)
                    .filter(it -> FeatureLookup.class.getName().equals(it.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .map(Object::toString)
                    .orElseGet(this::getDefaultFeatureLookupKey);
        }

    }

    /**
     * Default mock implementation of {@link FeatureLookup} interface.<br/>
     * Any lookup operation returns {@code false}.
     */
    class DefaultFeatureLookup implements FeatureLookup {

        @Override
        public boolean lookup(FeatureOptions options) {
            return false;
        }

        @Override
        public boolean lookup(
                FeatureOptionsBuilderProvider provider) {
            return false;
        }

        @Override
        public Mono<Boolean> lookupAsync(FeatureOptions options) {
            return Mono.just(Boolean.FALSE);
        }

        @Override
        public Mono<Boolean> lookupAsync(
                FeatureOptionsBuilderProvider provider) {
            return Mono.just(Boolean.FALSE);
        }

    }

}
