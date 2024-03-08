package com.feature.management;

import java.util.function.Supplier;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.server.WebFilter;

import com.bootstrap.feature.management.appconfig.CustomFeatureManager;
import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;
import com.feature.management.appconfig.FeatureLookupWebFilter;

/**
 * Reactive (weblux) web-based feature manager snapshot registration management autoconfiguration.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE,
        name = {"enabled", "snapshot-enabled"},
        havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class FeatureLookupWebFilterAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "com.azure.spring.cloud.feature.manager.FeatureManager")
    @ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
            havingValue = "APP_CONFIGURATION")
    public WebFilter featureLookupWebFilter(GenericApplicationContext context,
                                            WebEndpointProperties webEndpointProperties,
                                            Supplier<CustomFeatureManager> customFeatureManagerSnapshotSupplier) {
        return new FeatureLookupWebFilter(context, webEndpointProperties, customFeatureManagerSnapshotSupplier);
    }

}
