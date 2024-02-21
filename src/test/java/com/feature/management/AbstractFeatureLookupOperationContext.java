package com.feature.management;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import com.bootstrap.feature.management.FeatureLookup;
import com.bootstrap.feature.management.util.FeatureProxyUtils;

@ActiveProfiles({"test"})
public abstract class AbstractFeatureLookupOperationContext {

    @Autowired
    protected FeatureLookup featureLookup;

    @Autowired
    protected ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    protected Map<String, Boolean> getProvidedResponse(byte[] data) {
        try {
            return objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    interface TestController<T> {

        T ping(String featureName, String userId, String groupName);

        default Map<String, Boolean> on(String featureName) {
            return Map.of(featureName, true);
        }

        default Map<String, Boolean> off(String featureName) {
            return Map.of(featureName, false);
        }

    }

    @Profile("servlet")
    @RestController
    static class ServletContextTestController
            implements TestController<Map<String, Boolean>> {

        private final FeatureLookup featureLookup;

        public ServletContextTestController(FeatureLookup featureLookup) {
            this.featureLookup = featureLookup;
        }

        @Override
        @GetMapping("/validate/feature/{name}")
        public Map<String, Boolean> ping(
                @PathVariable("name") String featureName,
                @RequestParam(value = "user", required = false) String userId,
                @RequestParam(value = "group", required = false) String groupName) {
            return FeatureProxyUtils.featureProxyResolver(
                    featureLookup,
                    featureName,
                    StringUtils.hasText(userId) ? userId : null,
                    false,
                    StringUtils.hasText(groupName) ? Set.of(groupName) : Set.of(),
                    () -> off(featureName),
                    () -> on(featureName));
        }

    }

    @Profile("web-flux")
    @RestController
    static class ReactiveContextTestController
            implements TestController<Mono<Map<String, Boolean>>> {

        private final FeatureLookup featureLookup;

        public ReactiveContextTestController(FeatureLookup featureLookup) {
            this.featureLookup = featureLookup;
        }

        @Override
        @GetMapping("/validate/feature/{name}")
        public Mono<Map<String, Boolean>> ping(
                @PathVariable("name") String featureName,
                @RequestParam(value = "user", required = false) String userId,
                @RequestParam(value = "group", required = false) String groupName) {
            return FeatureProxyUtils.featureProxyResolverAsync(
                    featureLookup,
                    featureName,
                    StringUtils.hasText(userId) ? userId : null,
                    false,
                    StringUtils.hasText(groupName) ? Set.of(groupName) : Set.of(),
                    () -> Mono.just(off(featureName)),
                    () -> Mono.just(on(featureName)));
        }

    }

}
