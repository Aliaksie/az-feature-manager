package com.bootstrap.feature.management.local;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.azure.spring.cloud.feature.manager.FeatureManagementConfiguration;
import com.azure.spring.cloud.feature.manager.FeatureManager;
import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import com.bootstrap.feature.management.FeatureLookup;

@Configuration(value = "localConfigFeatureManagerConfiguration", proxyBeanMethods = false)
@ConditionalOnClass(name = "com.azure.spring.cloud.feature.manager.FeatureManager")
@ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
        havingValue = "LOCAL_CONFIGURATION")
@ComponentScan(basePackages = {"com.azure.spring.cloud.feature.manager", "com.bootstrap.feature.management.local"},
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {FeatureManager.class, FeatureManagementConfiguration.class})
        })
public class FeatureManagerConfiguration {

    private static final String FEATURES_SOURCE_PATH = "${az-feature-management.configuration.features}";

    @Bean
    public FeatureLookup featureLookup(Map<String, Feature> readFeatures) {
        return new DefaultFeatureLookupImpl(readFeatures);
    }

    @Bean
    Map<String, Feature> readFeatures(@Value(FEATURES_SOURCE_PATH) Resource script) {
        try {
            return new ObjectMapper().readValue(getFileContent(script.getFile().toPath()), new TypeReference<>() {
            });
        } catch (Exception e) {
            // SneakyThrows
            throw new RuntimeException(e);
        }
    }

    private String getFileContent(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            return reader
                    .lines()
                    .collect(Collectors.joining());
        } catch (Exception e) {
            // SneakyThrows
            throw new RuntimeException(e);
        }
    }

    private static class DefaultFeatureLookupImpl implements FeatureLookup {

        private final Map<String, FeatureManagerConfiguration.Feature> features;

        public DefaultFeatureLookupImpl(Map<String, Feature> features) {
            this.features = features;
        }

        @Override
        public boolean lookup(FeatureOptions options) {
            var feature = features.get(options.name());
            if (feature == null) {
                return options.defaultValue();
            }

            if (StringUtils.hasText(options.userId()) && !CollectionUtils.isEmpty(feature.users())) {
                return feature.enabled() && feature.users().contains(options.userId());
            }

            if (!CollectionUtils.isEmpty(options.groups()) && !CollectionUtils.isEmpty(feature.groups())) {
                return feature.enabled() && !Collections.disjoint(feature.groups(), options.groups());
            }

            return feature.enabled();
        }

        @Override
        public boolean lookup(
                FeatureOptionsBuilderProvider provider) {
            return lookup(provider.featureOptionsBuilder(DefaultFeatureOptions::builder).build());
        }

        @Override
        public Mono<Boolean> lookupAsync(FeatureOptions options) {
            return Mono.just(lookup(options));
        }

        @Override
        public Mono<Boolean> lookupAsync(
                FeatureOptionsBuilderProvider provider) {
            return Mono.just(lookup(provider.featureOptionsBuilder(DefaultFeatureOptions::builder).build()));
        }

    }

    private record Feature(boolean enabled,
                           Set<String> users,
                           Set<String> groups) {
        public Feature() {
            this(false, null, null);
        }
    }

}
