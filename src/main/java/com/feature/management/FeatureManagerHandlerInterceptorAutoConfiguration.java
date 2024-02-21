package com.feature.management;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;
import com.bootstrap.feature.management.FeatureLookup;

/**
 * Servlet web-based feature manager snapshot registration management autoconfiguration.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
@ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "enabled",
        havingValue = "true")
@ConditionalOnBean(type = "org.springframework.boot.web.servlet.server.ServletWebServerFactory",
        search = SearchStrategy.ALL)
class FeatureManagerHandlerInterceptorAutoConfiguration implements WebMvcConfigurer {

    private final FeatureManagerProperties featureManagerProperties;

    private final GenericApplicationContext context;

    private final Map<FeatureLookup.FeatureManagerType, Function<GenericApplicationContext, HandlerInterceptor>> interceptorsBindingMap;

    @Autowired
    FeatureManagerHandlerInterceptorAutoConfiguration(
            FeatureManagerProperties featureManagerProperties,
            GenericApplicationContext context,
            Map<FeatureLookup.FeatureManagerType, Function<GenericApplicationContext, HandlerInterceptor>> interceptorsBindingMap) {
        this.featureManagerProperties = featureManagerProperties;
        this.context = context;
        this.interceptorsBindingMap = interceptorsBindingMap;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (featureManagerProperties.snapshotEnabled()) {
            Optional.ofNullable(featureManagerProperties.getType())
                    .filter(FeatureLookup.FeatureManagerType::isSupported)
                    .map(interceptorsBindingMap::get)
                    .map(interceptor -> interceptor.apply(context))
                    .ifPresent(registry::addInterceptor);
        }
    }

}
