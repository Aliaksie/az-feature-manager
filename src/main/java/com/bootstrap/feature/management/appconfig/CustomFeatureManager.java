package com.bootstrap.feature.management.appconfig;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.azure.spring.cloud.feature.manager.FeatureManagementConfigProperties;
import com.azure.spring.cloud.feature.manager.FeatureManager;
import com.azure.spring.cloud.feature.manager.FilterNotFoundException;
import com.azure.spring.cloud.feature.manager.entities.Feature;
import com.azure.spring.cloud.feature.manager.entities.FeatureFilterEvaluationContext;
import reactor.core.publisher.Mono;

import com.bootstrap.feature.management.FeatureLookup;

/**
 * Enhanced {@linkplain FeatureManager} decorator type accepting specified {@linkplain FeatureLookup.FeatureOptions} parameter in
 * serialized shape.
 */
public class CustomFeatureManager extends HashMap<String, Object> {

    /**
     * Default {@linkplain CustomFeatureManager} root bean name.
     */
    public static final String DEFAULT_FEATURE_MANAGEMENT_KEY = "customFeatureManager";

    private static final VarHandle FEATURE_MANAGEMENT_FIELD = getVarHandle("featureManagement", Map.class);

    private final FeatureManager featureManager;

    CustomFeatureManager(FeatureManagementConfigProperties properties) {
        this(properties, null);
    }

    /**
     * Suppress warning as we are trying to designate particular constructor as autowired.
     *
     * @param properties           feature management configuration properties holder
     * @param customFeatureManager provider to the shared manager instance or null
     */
    CustomFeatureManager(
            FeatureManagementConfigProperties properties,
            CustomFeatureManager customFeatureManager) {
        this.featureManager = Optional.ofNullable(customFeatureManager)
                .<FeatureManager>map(it -> new FeatureManagerDecorator(properties, getFeatures(it)))
                .orElseGet(() -> getDefaultFeatureManager(properties));
    }

    /**
     * Creates instance of {@linkplain CustomFeatureManager} bean type.
     *
     * @param properties           feature management configuration properties holder
     * @param customFeatureManager provider to the shared manager instance or null
     * @return reference to the shared {@linkplain CustomFeatureManager} manager instance
     */
    public static CustomFeatureManager create(
            FeatureManagementConfigProperties properties,
            CustomFeatureManager customFeatureManager) {
        return new CustomFeatureManager(properties, customFeatureManager);
    }

    /**
     * Checks to verify expressed in terms of {@linkplain FeatureLookup.FeatureOptions} feature is existed/enabled. If enabled it
     * walks through each particular filter, once a single filter returns true it returns true. If no filter returns
     * true, it returns false. If there are no filters, it returns true. If feature isn't found it returns false.
     *
     * @param feature {@linkplain FeatureLookup.FeatureOptions} serialized/stringified representation.
     * @return evaluated state of the specified feature configuration
     */
    public Mono<Boolean> isEnabledAsync(String feature) throws FilterNotFoundException {
        var context = FeatureOptionsImpl.builder().fromString(feature);

        return featureManager.isEnabledAsync(feature)
                .zipWith(Mono.fromSupplier(() -> evaluate(context)))
                .map(it -> it.getT1() || it.getT2());
    }

    private boolean evaluate(FeatureLookup.FeatureEvaluationContext<FeatureFilterEvaluationContext> context) {
        @SuppressWarnings("unchecked")
        var featureManagement = (Map<String, Feature>) FEATURE_MANAGEMENT_FIELD.get(featureManager);
        var feature = featureManagement.get(context.name());

        if (feature == null) {
            return false;
        }

        return context.evaluate(feature.getEnabledFor().values());
    }

    private FeatureManager getDefaultFeatureManager(FeatureManagementConfigProperties properties) {
        return new FeatureManager(properties);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Feature> getFeatures(CustomFeatureManager featureManager) {
        return Optional.ofNullable(featureManager)
                .map(CustomFeatureManager::getFeatureManager)
                .map(FEATURE_MANAGEMENT_FIELD::get)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .orElseGet(HashMap::new);
    }

    /**
     * Suppress lost exception warning in favor of using exception builder pattern.
     *
     * @param field     class type field name to lookup from the accessible context
     * @param fieldType field type
     * @return field var handle object
     */
    private static VarHandle getVarHandle(String field, Class<?> fieldType) {
        try {
            return MethodHandles
                    .privateLookupIn(FeatureManager.class, MethodHandles.lookup())
                    .findVarHandle(FeatureManager.class, field, fieldType);
        } catch (Exception e) {
            throw new VarHandleInstantiationException(e);
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> properties) {
        featureManager.putAll(properties);
    }

    private static class VarHandleInstantiationException extends RuntimeException {

        private final Exception cause;

        public VarHandleInstantiationException(Exception cause) {
            super(cause);
            this.cause = cause;
        }

    }

    private static class FeatureManagerDecorator extends FeatureManager {

        /**
         * Used to evaluate whether a feature is enabled or disabled.
         *
         * @param properties Configuration options for Feature Management
         */
        FeatureManagerDecorator(FeatureManagementConfigProperties properties, Map<String, Feature> features) {
            super(properties);
            initFeatureManagement(features);
        }

        private void initFeatureManagement(Map<String, Feature> features) {
            @SuppressWarnings("unchecked")
            var featureManagement = (Map<String, Feature>) FEATURE_MANAGEMENT_FIELD.get(this);
            if (featureManagement != null) {
                Optional.ofNullable(features)
                        .ifPresent(featureManagement::putAll);
            }
        }

        /**
         * Suppress warning as no-op implementation is intentionally required to stop refresh propagation.
         *
         * @param properties feature management properties
         */
        @Override
        @SuppressWarnings("squid:S1186")
        public void putAll(Map<? extends String, ? extends Object> properties) {

        }

    }

    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomFeatureManager that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(featureManager, that.featureManager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), featureManager);
    }

}
