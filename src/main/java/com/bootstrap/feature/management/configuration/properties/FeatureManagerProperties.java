package com.bootstrap.feature.management.configuration.properties;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.bootstrap.feature.management.FeatureLookup;

/**
 * Basic Feature Management properties holder.
 */

@ConfigurationProperties(FeatureManagerProperties.PROPERTY_SOURCE_NAMESPACE)
public record FeatureManagerProperties(
        boolean enabled,
        boolean snapshotEnabled,
        FeatureLookup.FeatureManagerType type,
        @NotBlank
        String connectionString) {

    /**
     * Management properties default namespace.
     */
    public static final String PROPERTY_SOURCE_NAMESPACE = "az-feature-management.configuration";


    public FeatureLookup.FeatureManagerType getType() {
        return Optional.ofNullable(type)
                .orElse(FeatureLookup.FeatureManagerType.UNSPECIFIED);
    }

}
