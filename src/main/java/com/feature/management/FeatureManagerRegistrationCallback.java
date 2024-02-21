package com.feature.management;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Simple feature manager snapshot registration management contract to adhere/extend. By default leverages spring
 * context runtime bean registration option.
 *
 * @param <T> feature manager specific type
 * @param <C> feature manager registration context specific type
 */
public interface FeatureManagerRegistrationCallback<T, C> {

    /**
     * Feature manager context key/alias.
     */
    String FEATURE_MANAGER_KEY_TEMPLATE = "feature.manager.%d";

    /**
     * Get feature manager registration context.
     *
     * @return feature manager registration context
     */
    C getContext();

    /**
     * Implementation specific callback to use when extra logic is required to reset/fallback to non-snapshot variation.
     */
    void useDefaultFeatureManagerCallback();

    /**
     * Feature manager specific implementation type.
     *
     * @return feature manager type
     */
    Class<T> getFeatureManagerType();

    /**
     * Register Feature manager callback function.
     *
     * @param featureManagerKey Feature manager context key/alias
     */
    void registerFeatureManager(String featureManagerKey);

    /**
     * Unregister Feature manager callback function.
     *
     * @param featureManagerKey Feature manager context key/alias
     */
    void unregisterFeatureManager(String featureManagerKey);

    /**
     * Generate Feature manager context key/alias.
     *
     * @param postfix context identifier
     * @return Feature manager context key/alias
     */
    static String getFeatureManagerKey(int postfix) {
        return String.format(FEATURE_MANAGER_KEY_TEMPLATE, postfix);
    }

    /**
     * Spring context aware feature manager registration callback variation.
     *
     * @param <T> feature manager specific type.
     */
    interface ApplicationContextAwareFeatureManagerRegistrationCallback<T>
            extends FeatureManagerRegistrationCallback<T, GenericApplicationContext> {

        /**
         * Keeps track UNREGISTERED_FEATURE_MANAGER_NAMES.
         */
        @SuppressWarnings("squid:S2386")
        List<String> UNREGISTERED_FEATURE_MANAGER_NAMES = new CopyOnWriteArrayList<>();

        /**
         * FeatureManagerCallback.
         *
         * @return supplier
         */
        Supplier<T> getFeatureManagerCallback();

        @Override
        default void registerFeatureManager(String featureManagerKey) {
            onRegisterCallbackHook();

            if (StringUtils.hasText(featureManagerKey)) {
                try {
                    getContext().registerBean(featureManagerKey, getFeatureManagerType(), getFeatureManagerCallback(),
                            bd -> {
                                bd.setAutowireCandidate(false);
                                bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                            });
                } catch (Exception e) {
                    onErrorRegisterCallbackHook(featureManagerKey);
                }
            }
        }

        @Override
        default void unregisterFeatureManager(String featureManagerKey) {
            onUnregisterCallbackHook();
            GenericApplicationContext context = getContext();

            try {
                Optional.ofNullable(featureManagerKey)
                        .filter(context::containsBeanDefinition)
                        .ifPresent(context::removeBeanDefinition);
            } catch (Exception e) {
                onErrorUnregisterCallbackHook(featureManagerKey);
            }
        }

        /**
         * No-op default callback variant.
         */
        default void onRegisterCallbackHook() {
            UNREGISTERED_FEATURE_MANAGER_NAMES.forEach(this::unregisterFeatureManager);
            UNREGISTERED_FEATURE_MANAGER_NAMES.clear();
        }

        /**
         * No-op default callback variant.
         *
         * @param featureManagerKey Feature manager context key/alias
         */
        default void onErrorRegisterCallbackHook(String featureManagerKey) {
            useDefaultFeatureManagerCallback();
        }

        /**
         * No-op default callback variant.
         */
        default void onUnregisterCallbackHook() {

        }

        /**
         * No-op default callback variant.
         *
         * @param featureManagerKey Feature manager context key/alias
         */
        default void onErrorUnregisterCallbackHook(String featureManagerKey) {
            UNREGISTERED_FEATURE_MANAGER_NAMES.add(featureManagerKey);
        }

    }

}
