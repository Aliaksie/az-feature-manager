# Feature Toggles Management library

## Table of Contents

- [Overview](#overview)
  - [Every Feature Toggle](#every-feature-toggle-description-object-should-be-equipped-with-required-lookup-metadata-reflecting-and-revealing-the-following-information)
  - [How to add](#in-order-to-add-feature-toggles-management-facade-library-to-the-project-follow-below-steps)
  - [Servlet web-based example](#servlet-web-based-application-usage-example)
  - [Reactive web-based example](#reactive-web-based-application-usage-example)
- [Parent documentation](#parent-documentation)

## Overview

#### Feature Toggles Management facade library intended to unify interaction model with possibly different implementation providers.
#### Current library module is designed to provide basic capabilities and a common conventions set for feature management to being able to express/prescribe feature toggle object to lookup and validate (current version is mainly biased to the AppConfiguration Feature Manager variation).
#### Every Feature Toggle description object should be equipped with required lookup metadata reflecting and revealing the following information:

- featureLookupKey - feature manager key/alias in terms of underlying cache specific implementation to locate
  snapshot/cached instance in order for ensure that feature flags should be consistent across the entire request as
  configuration values can change in real-time;

- name - name of evaluated Feature Toggle object/instance real data owner - do not confuse with above property - ownerId
  is key property and used as a partition key for the audit store Cosmos container;

- userId - reflects user specific identifier attribute value;

- groups - used to evaluate specified feature flag on user to belong to a certain group condition;

- defaultValue - default evaluation value for specified feature flag.

#### In order to add Feature Toggles Management facade library to the project follow below steps:

- add library dependency to the required project;

- Turn on feature lookup functionality by placing below configuration properties set under bootstrap configuration file
  for specific project:

```
az-feature-management:
  configuration:
    enabled: true
    snapshot-enabled: true
    type: APP_CONFIGURATION
```

#### Servlet web-based application usage example:

```
private final FeatureLookup featureLookup;

@PostMapping("/resolve")
@PreAuthorize("@verifyAccess(authentication)")
public ExampleDTO resolveIdentity(@RequestBody @Validated ExampleRequest request) {
    FeatureLookup.FeatureOptionsBuilderProvider options = builder -> builder
            .featureLookupKey(FeatureManagementEnvironmentType.WEB_SERVLET_BLOCKING.getFeatureLookupKey(Stream::empty))
            .name("TestFeatureEnabled")
            .userId(request.getId())
            .defaultValue(true)
            .groups(Set.of());

    if (featureLookup.lookup(options)) {
        return exampleService.resolveExample(request);
    } else {
        throw new IllegalArgumentException();
    }
}
```

#### Reactive web-based application usage example:

```
private final FeatureLookup featureLookup;

public Mono<Example> fetchExample(String id, DataFetchingEnvironment env) {
    return authenticationRepository.getAuthentication(GraphQLUtils.getExchange(env))
            .transformDeferredContextual((auth, ctx) ->
                auth.flatMap(principal -> {
                    String exampleId = Optional.ofNullable(id).orElseGet(principal::getId);

                    FeatureLookup.FeatureOptionsBuilderProvider options = builder -> builder
                            .featureLookupKey(FeatureManagementEnvironmentType.WEB_REACTIVE
                                    .getFeatureLookupKey(ctx::stream))
                            .name("TestFeatureEnabled")
                            .userId(userId)
                            .defaultValue(true)
                            .groups(Set.of());

                    return featureLookup.lookupAsync(options)
                            .flatMap(isTestFeatureEnabled -> {
                                if (isTestFeatureEnabled) {
                                    return exampleCrudApi.findExampleByIdUsingGET(principal.getRunAsHeader(), exampleId);
                                } else {
                                    throw new IllegalArgumentException();
                                }
                            });
                }));
}
```

#### In order to override remote value of feature and use you own local value:

- update service bootstrap-local.yaml/bootstrap.yaml
```
    az-feature-management:
        configuration:
            enabled: true
            type: LOCAL_CONFIGURATION
            features: ./path_to_features.json
```

- add under app resources folder you own features like './sample_features.json'

