package com.feature.management.unit;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.appconfig.CustomFeatureManager;
import com.bootstrap.feature.management.appconfig.FeatureLookupImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeatureLookupImplTests {

    private static final CustomFeatureManager customFeatureManager = mock(CustomFeatureManager.class);

    private static final ApplicationContext applicationContext = mock(ApplicationContext.class);

    private static final FeatureLookup.FeatureOptions featureOptions = mock(FeatureLookup.FeatureOptions.class);

    private final FeatureLookup featureLookup = new FeatureLookupImpl(null, applicationContext, true);

    @BeforeAll
    static void init() {
        when(featureOptions.featureLookupKey()).thenReturn("featureLookupKey");
        when(featureOptions.name()).thenReturn("featureName");
        when(featureOptions.groups()).thenReturn(Set.of());
        when(featureOptions.userId()).thenReturn("userId");
        when(featureOptions.defaultValue()).thenReturn(false);

        when(applicationContext.containsBeanDefinition("featureLookupKey")).thenReturn(true);
        when(applicationContext.getBean("featureLookupKey", CustomFeatureManager.class)).thenReturn(
                customFeatureManager);
    }

    @Test
    @DisplayName("Verifies: featureLookup method returns true if customFeatureManager#isEnabledAsync returns true")
    void shouldReturnTrueWhenIsEnabledAsyncTrue() {
        when(customFeatureManager.isEnabledAsync(anyString())).thenReturn(Mono.just(true));

        boolean lookupResult = featureLookup.lookup(featureOptions);

        assertThat(lookupResult).isTrue();
    }

    @Test
    @DisplayName("Verifies: featureLookup method returns true if customFeatureManager#isEnabledAsync returns true")
    void shouldReturnFalseWhenIsEnabledAsyncFalse() {
        when(customFeatureManager.isEnabledAsync(anyString())).thenReturn(Mono.just(false));

        boolean lookupResult = featureLookup.lookup(featureOptions);

        assertThat(lookupResult).isFalse();
    }

    @Test
    @DisplayName("Verifies: featureLookup method throws exception if customFeatureManager#isEnabledAsync fails and throws exception")
    void shouldThrowExceptionWhenIsEnabledAsyncThrows() {
        when(customFeatureManager.isEnabledAsync(anyString()))
                .thenThrow(new RuntimeException("Failed to determine if feature is enabled async."));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> featureLookup.lookup(featureOptions));
    }

}
