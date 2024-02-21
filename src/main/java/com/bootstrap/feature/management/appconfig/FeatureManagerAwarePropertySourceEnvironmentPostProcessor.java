package com.bootstrap.feature.management.appconfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.azure.spring.cloud.config.properties.AppConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;
import com.bootstrap.feature.management.FeatureLookup.FeatureManagerType;

class FeatureManagerAwarePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String FEATURE_MANAGEMENT_TYPE_PROPERTY_NAME = String.join(".",
            FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, "type");

    private static final String FEATURE_MANAGEMENT_ENABLED_PROPERTY_NAME = String.join(".",
            FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, "enabled");

    private static final String FEATURE_MANAGEMENT_STORE_PROPERTY_PREFIX = String.join(".",
            AppConfigurationProperties.CONFIG_PREFIX, "stores");

    private static final String FEATURE_MANAGEMENT_ENABLED_PROPERTY_TEMPLATE = String.join(".",
            AppConfigurationProperties.CONFIG_PREFIX, "stores[%d]", "feature-flags", "enabled");

    private static final Pattern FEATURE_MANAGEMENT_STORE_PROPERTY_PATTERN = Pattern
            .compile("(" + (Pattern.quote(FEATURE_MANAGEMENT_STORE_PROPERTY_PREFIX) + "(?:\\[\\d+?]))(?:.*)"));

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources sources = environment.getPropertySources();
        PropertySource<?> existing = sources.get(AppConfigurationProperties.CONFIG_PREFIX);
        if (existing != null) {
            return;
        }
        boolean enabled = environment.getProperty(FEATURE_MANAGEMENT_ENABLED_PROPERTY_NAME, Boolean.class,
                Boolean.FALSE);
        FeatureManagerType type = environment.getProperty(FEATURE_MANAGEMENT_TYPE_PROPERTY_NAME,
                FeatureManagerType.class, FeatureManagerType.UNSPECIFIED);

        String[] locations = environment.getProperty("spring.cloud.bootstrap.location", String[].class);

        var sourceName = Optional.ofNullable(locations)
                .map(Arrays::asList)
                .map(CollectionUtils::lastElement)
                .orElse("bootstrap.y");

        @SuppressWarnings({"unchecked", "squid:S1905"})
        var bootstrapSource = (Map<String, Object>) sources.stream()
                .filter(MapPropertySource.class::isInstance)
                .filter(it -> it.getName().contains(sourceName))
                .findFirst()
                .filter(Objects::nonNull)
                .map(PropertySource::getSource)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .orElseGet(Collections::emptyMap);

        if (enabled && FeatureManagerType.APP_CONFIGURATION == type && !bootstrapSource.isEmpty()) {
            MapPropertySource propertySource = getFeatureManagerAwarePropertySource(
                    bootstrapSource);
            if (sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) != null) {
                sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
            } else {
                sources.addLast(propertySource);
            }
        }
    }

    private MapPropertySource getFeatureManagerAwarePropertySource(
            Map<String, Object> bootstrapSource) {
        var stores = bootstrapSource.keySet()
                .stream()
                .filter(it -> it.startsWith(FEATURE_MANAGEMENT_STORE_PROPERTY_PREFIX))
                .map(it -> it.replaceAll(FEATURE_MANAGEMENT_STORE_PROPERTY_PATTERN.pattern(), "$1"))
                .distinct()
                .count();

        Set<Map.Entry<String, Object>> featureManagementAwareEntries = LongStream.range(0, stores)
                .mapToObj(it -> String.format(FEATURE_MANAGEMENT_ENABLED_PROPERTY_TEMPLATE, it))
                .map(it -> Map.<String, Object>entry(it, Boolean.TRUE))
                .collect(Collectors.toUnmodifiableSet());

        Map<String, Object> sourceProperties = Stream.of(featureManagementAwareEntries, bootstrapSource.entrySet())
                .flatMap(Set::stream)
                .filter(it -> it.getKey().startsWith(FEATURE_MANAGEMENT_STORE_PROPERTY_PREFIX))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Function.<Map.Entry<String, Object>>identity().andThen(Map.Entry::getValue)
                                .andThen(Objects::toString),
                        (left, right) -> right));

        return new OriginTrackedMapPropertySource(AppConfigurationProperties.CONFIG_PREFIX, sourceProperties);
    }

}
