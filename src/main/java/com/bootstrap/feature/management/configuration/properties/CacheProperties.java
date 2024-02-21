package com.bootstrap.feature.management.configuration.properties;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import static com.bootstrap.feature.management.configuration.properties.CacheProperties.PROPERTY_SOURCE_NAMESPACE;

@ConfigurationProperties(PROPERTY_SOURCE_NAMESPACE)
public record CacheProperties(Boolean enabled, InMemoryCacheProperties inMemory) {
    /**
     * Cache properties default namespace.
     */
    public static final String PROPERTY_SOURCE_NAMESPACE = "cache.configuration";

    public CacheProperties {
        if (enabled == null) enabled = Boolean.TRUE;
        if (inMemory == null) inMemory = new InMemoryCacheProperties();
    }

    @Validated
    public record InMemoryCacheProperties(
            @Min(8) @Max(128) Integer min,
            @Min(16) @Max(4096) Integer max,
            @DecimalMin("0.50") @DecimalMax("1.00") @Digits(integer = 1, fraction = 2) @Positive BigDecimal ratio) {

        public InMemoryCacheProperties() {
            this(64, 2048, null);
        }

        public InMemoryCacheProperties {
            if (min == null) min = 64;
            if (max == null) min = 2048;
        }

        public float getRatio() {
            return Optional.ofNullable(ratio)
                    .map(BigDecimal::floatValue)
                    .orElse(0.75f);
        }

    }

}
