package com.bootstrap.feature.management.configuration;

import com.azure.spring.cloud.feature.manager.FeatureManagementConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.bootstrap.feature.management.configuration.properties.CacheProperties;
import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;

/**
 * Feature Management default initializer configuration.
 */
@Configuration
@EnableConfigurationProperties({ FeatureManagerProperties.class, FeatureManagementConfigProperties.class,
        CacheProperties.class })
public class FeatureManagerConfiguration {

}
