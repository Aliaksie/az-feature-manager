package com.feature.management.appconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;

import com.azure.spring.cloud.config.AppConfigurationRefresh;
import com.azure.spring.cloud.config.implementation.AppConfigurationPullRefresh;
import com.azure.spring.cloud.config.implementation.AppConfigurationReplicaClientFactory;
import com.azure.spring.cloud.config.properties.AppConfigurationProperties;
import com.azure.spring.cloud.config.properties.AppConfigurationProviderProperties;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.appconfig.CustomFeatureManager;
import com.bootstrap.feature.management.configuration.properties.FeatureManagerProperties;

/**
 * App Configuration feature manager snapshot registration management autoconfiguration.
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.azure.spring.cloud.feature.manager.FeatureManager")
@ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "enabled",
        havingValue = "true")
public class AppConfigFeatureManagerAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class AppConfigFeatureManagerAdapterConfiguration {
        @Bean("FeatureManagement")
        @ConfigurationProperties(prefix = "feature-management")
        @ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
                havingValue = "APP_CONFIGURATION")
        public Map<String, Object> featureManager(CustomFeatureManager customFeatureManager) {
            return new HashMap<>() {

                @Override
                public void putAll(Map<? extends String, ? extends Object> properties) {
                    customFeatureManager.putAll(properties);
                }

            };
        }

        @Bean
        @ConditionalOnMissingBean
        public AppConfigurationRefresh appConfigurationRefresh(AppConfigurationProperties properties,
                                                               AppConfigurationProviderProperties appProperties,
                                                               AppConfigurationReplicaClientFactory clientFactory) {
            return new AppConfigurationPullRefresh(clientFactory, properties.getRefreshInterval(),
                    appProperties.getDefaultMinBackoff());
        }

        @Bean("readinessStateHealthIndicator")
        @ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
                havingValue = "APP_CONFIGURATION")
        public HealthIndicator refreshHealthIndicator(AppConfigurationRefresh refreshObjectProvider,
                                                      ApplicationAvailability applicationAvailability) {
            return new FeatureManagerRefreshHealthIndicator(refreshObjectProvider, applicationAvailability);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
                havingValue = "APP_CONFIGURATION")
        public TaskExecutor appConfigFeatureManagerTaskExecutor() {
            var taskExecutor = new ThreadPoolTaskExecutor();
            var poolSize = Runtime.getRuntime().availableProcessors();
            taskExecutor.setCorePoolSize(poolSize);
            taskExecutor.setMaxPoolSize(poolSize);
            taskExecutor.setDaemon(true);
            taskExecutor.setQueueCapacity(1_000);
            taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            taskExecutor.setAllowCoreThreadTimeOut(true);

            return taskExecutor;
        }
    }

    /**
     * FeatureWebMvcInterceptorsBindingConfiguration.
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class FeatureWebMvcInterceptorsBindingConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE, name = "type",
                havingValue = "APP_CONFIGURATION")
        public Map<FeatureLookup.FeatureManagerType, Function<GenericApplicationContext, HandlerInterceptor>> interceptorsBindingMap(
                Supplier<CustomFeatureManager> customFeatureManagerSnapshotSupplier) {
            return Map.of(FeatureLookup.FeatureManagerType.APP_CONFIGURATION,
                    ctx -> new FeatureManagerHandlerInterceptor(ctx, customFeatureManagerSnapshotSupplier));
        }

    }

    private static class FeatureManagerRefreshHealthIndicator extends ReadinessStateHealthIndicator {

        private final AppConfigurationRefresh refreshObjectProvider;

        FeatureManagerRefreshHealthIndicator(
                AppConfigurationRefresh refreshObjectProvider,
                ApplicationAvailability availability) {
            super(availability);
            this.refreshObjectProvider = refreshObjectProvider;
        }

        @Override
        public AvailabilityState getState(ApplicationAvailability applicationAvailability) {
            try {
                refreshObjectProvider.refreshConfigurations();
                return super.getState(applicationAvailability);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
