package com.bootstrap.feature.management.appconfig;

import java.io.Serializable;
import java.util.function.Supplier;

import com.azure.spring.cloud.feature.manager.FeatureManagementConfigProperties;
import com.azure.spring.cloud.feature.manager.FeatureManagementConfiguration;
import com.azure.spring.cloud.feature.manager.FeatureManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.cache.Cache;
import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;

@Configuration(value = "appConfigFeatureManagerConfiguration", proxyBeanMethods = false)
@ConditionalOnClass(name = "com.azure.spring.cloud.feature.manager.FeatureManager")
@ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
        havingValue = "APP_CONFIGURATION")
@ComponentScan(
        basePackages = { "com.azure.spring.cloud.feature.manager",
                "com.bootstrap.feature.management.appconfig" },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = { FeatureManager.class, FeatureManagementConfiguration.class })
        })
public class FeatureManagerConfiguration {

    @Primary
    @Bean(CustomFeatureManager.DEFAULT_FEATURE_MANAGEMENT_KEY)
    public CustomFeatureManager customFeatureManager(FeatureManagementConfigProperties properties) {
        return new CustomFeatureManager(properties);
    }

    @Bean
    public Supplier<CustomFeatureManager> customFeatureManagerSnapshotSupplier(
            FeatureManagementConfigProperties featureManagementConfigProperties,
            @Qualifier(CustomFeatureManager.DEFAULT_FEATURE_MANAGEMENT_KEY) CustomFeatureManager customFeatureManager) {
        return () -> CustomFeatureManager.create(featureManagementConfigProperties, customFeatureManager);
    }

    @Bean
    public FeatureLookup featureLookup(
            Cache<String, Serializable> cache,
            ApplicationContext context,
            FeatureManagerProperties featureManagerProperties) {
        return new FeatureLookupImpl(cache, context, featureManagerProperties.snapshotEnabled());
    }

}
