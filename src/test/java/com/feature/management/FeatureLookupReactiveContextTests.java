package com.feature.management;

import java.util.Map;

import jakarta.annotation.PostConstruct;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("web-flux")
@AutoConfigureWebTestClient
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@WireMockTest(httpPort = 8070)
@DisplayName("Verifies Feature Lookup operation within reactive context environment.")
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = {ServletWebServerFactory.class}))
@TestPropertySource(properties = {"spring.main.web-application-type=reactive", "server.port=8080",
        "spring.cloud.bootstrap.enabled=true"})
class FeatureLookupReactiveContextTests extends AbstractFeatureLookupOperationContext {

    static {
        System.setProperty("spring.cloud.bootstrap.location", "classpath:/,classpath:bootstrap-web-flux.yaml");
    }

    @Autowired
    private GenericApplicationContext context;

    @Autowired
    private PropertySourceBootstrapConfiguration propertySourceBootstrapConfiguration;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private WebTestClient webClient;

    @PostConstruct
    void bootstrap() {
        propertySourceBootstrapConfiguration.initialize(context);
        publisher.publishEvent(new RefreshEvent(this, null, ""));
    }

    @Test
    @DisplayName("Verifies: 'test_feature' is enabled with no users/groups filters specified.")
    void shouldReturnFeatureOnManagementState() throws Exception {

        var featureName = "test_feature_1";
        var userId = "test_user_id";

        WebTestClient.ResponseSpec result = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(String.format("/validate/feature/%s", featureName))
                        .queryParam("user", userId)
                        .build())
                .exchange();

        result.expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().consumeWith(data -> {
                    var provided = getProvidedResponse(data.getResponseBody());
                    var expected = Map.of(featureName, true);

                    assertThat(provided).isEqualTo(expected);
                });
    }

    @Profile("web-flux")
    @SpringBootApplication
    @EnableConfigurationProperties({WebEndpointProperties.class})
    static class TestReactiveContextApplication {

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            var javaTimeModule = new JavaTimeModule();
//            javaTimeModule.addSerializer(Instant.class, new TruncatedInstantSerializer());
            return new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(javaTimeModule)
//                    .registerModule(new JsonNullableModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Bean
        public AbstractReactiveWebServerFactory reactiveWebServerFactory(@Value("${server.port}") int port) {
            return new NettyReactiveWebServerFactory(port);
        }

        public static void main(String[] args) {
            SpringApplication.run(TestReactiveContextApplication.class, args);
        }

    }

}
