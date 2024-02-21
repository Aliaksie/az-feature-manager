package com.feature.management.appconfig;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.feature.management.FeatureManagerRegistrationCallback;
import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.appconfig.CustomFeatureManager;

/**
 * FeatureManagerHandlerInterceptor for azure app_config.
 */

public class FeatureManagerHandlerInterceptor implements HandlerInterceptor,
        FeatureManagerRegistrationCallback.ApplicationContextAwareFeatureManagerRegistrationCallback<CustomFeatureManager> {
    private GenericApplicationContext context;
    private Supplier<CustomFeatureManager> featureManagerCallback;

    public FeatureManagerHandlerInterceptor(GenericApplicationContext context,
                                            Supplier<CustomFeatureManager> featureManagerCallback) {
        this.context = context;
        this.featureManagerCallback = featureManagerCallback;

    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod
                && request.getDispatcherType() == DispatcherType.REQUEST
                && isValidStatus(response)) {

            var featureManagerKey = FeatureManagerRegistrationCallback.getFeatureManagerKey(request.hashCode());
            var attributes = RequestContextHolder.currentRequestAttributes();
            attributes.setAttribute(FeatureLookup.class.getName(), featureManagerKey,
                    RequestAttributes.SCOPE_REQUEST);

            registerFeatureManager(featureManagerKey);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView view) {
        if (handler instanceof HandlerMethod
                && request.getDispatcherType() == DispatcherType.REQUEST
                && isValidStatus(response)) {

            var featureManagerKey = FeatureManagerRegistrationCallback.getFeatureManagerKey(request.hashCode());

            unregisterFeatureManager(featureManagerKey);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception e) {
        if (handler instanceof HandlerMethod && request.getDispatcherType() == DispatcherType.REQUEST) {
            var featureManagerKey = FeatureManagerRegistrationCallback.getFeatureManagerKey(request.hashCode());

            unregisterFeatureManager(featureManagerKey);
        }
    }

    @Override
    public GenericApplicationContext getContext() {
        return Optional.ofNullable(context)
                .map(ApplicationContext::getParent)
                .filter(GenericApplicationContext.class::isInstance)
                .map(GenericApplicationContext.class::cast)
                .orElse(context);
    }

    @Override
    public void useDefaultFeatureManagerCallback() {
        var attributes = RequestContextHolder.currentRequestAttributes();
        attributes.removeAttribute(FeatureLookup.class.getName(), RequestAttributes.SCOPE_REQUEST);
    }

    @Override
    public Class<CustomFeatureManager> getFeatureManagerType() {
        return CustomFeatureManager.class;
    }

    private boolean isValidStatus(HttpServletResponse response) {
        try {
            return Optional.ofNullable(response)
                    .map(HttpServletResponse::getStatus)
                    .map(HttpStatus::valueOf)
                    .filter(Predicate.not(HttpStatus::isError)
                            .and(Predicate.not(HttpStatus::is4xxClientError))
                            .and(Predicate.not(HttpStatus::is5xxServerError)))
                    .isPresent();
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public Supplier<CustomFeatureManager> getFeatureManagerCallback() {
        return featureManagerCallback;
    }
}
