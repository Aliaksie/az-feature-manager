package com.bootstrap.feature.management;

import java.io.Serializable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.bootstrap.feature.management.cache.Cache;
import com.bootstrap.feature.management.configuration.properties.CacheProperties;
import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;

/**
 * Bootstrap configuration type placeholder responsible for loading Blockhound instrumentation agent during spring cloud
 * bootstrap context phase lifecycle.
 */
@Configuration
@ComponentScan("com.bootstrap.feature.management")
@ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FeatureManagerBootstrapConfiguration {

    @Bean
    public FactoryBean<Cache<String, Serializable>> cacheFactoryBean(CacheProperties cacheProperties) {
        return new Cache.CacheFactory<>(cacheProperties);
    }

}
