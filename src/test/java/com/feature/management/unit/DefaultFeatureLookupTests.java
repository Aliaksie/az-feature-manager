package com.feature.management.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;

import com.bootstrap.feature.management.FeatureLookup;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verifies: DefaultFeatureLookup implementation returns false to each method call")
class DefaultFeatureLookupTests {

    @Mock
    private FeatureLookup.FeatureOptions featureOptions;

    @Mock
    private FeatureLookup.FeatureOptionsBuilderProvider featureOptionsBuilderProvider;

    private final FeatureLookup featureLookup = new FeatureLookup.DefaultFeatureLookup();

    @Test
    void testLookupShouldReturnFalseWhenFeatureOptionsProvided() {
        boolean lookupResult = featureLookup.lookup(featureOptions);

        assertThat(lookupResult).isFalse();
    }

    @Test
    void testLookupShouldReturnFalseWhenFeatureOptionsBuilderProviderProvided() {
        boolean lookupResult = featureLookup.lookup(featureOptionsBuilderProvider);

        assertThat(lookupResult).isFalse();
    }

    @Test
    void testLookupAsyncShouldReturnFalseWhenFeatureOptionsProvided() {
        Mono<Boolean> lookupResult = featureLookup.lookupAsync(featureOptions);

        lookupResult.subscribe(result -> assertThat(result).isFalse());
    }

    @Test
    void testLookupAsyncShouldReturnFalseWhenFeatureOptionsBuilderProviderProvided() {
        Mono<Boolean> lookupResult = featureLookup.lookupAsync(featureOptionsBuilderProvider);

        lookupResult.subscribe(result -> assertThat(result).isFalse());
    }

}
