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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ActiveProfiles("servlet")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@WireMockTest(httpPort = 8070)
@DisplayName("Verifies Feature Lookup operation within servlet context environment.")
@TestPropertySource(properties = {"spring.main.web-application-type=servlet", "server.port=8081", "spring.cloud.bootstrap.enabled=true"})
class FeatureLookupServletContextTests extends AbstractFeatureLookupOperationContext {

    static {
        System.setProperty("spring.cloud.bootstrap.location", "classpath:/,classpath:bootstrap-servlet.yaml");
    }

    @Autowired
    private GenericApplicationContext context;

    @Autowired
    private PropertySourceBootstrapConfiguration propertySourceBootstrapConfiguration;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private MockMvc mockMvc;

    @PostConstruct
    void bootstrap() {
        propertySourceBootstrapConfiguration.initialize(context);
        publisher.publishEvent(new RefreshEvent(this, null, ""));
    }

    @Test
    @DisplayName("Verifies: 'test_feature' is enabled with no users/groups filters specified.")
    void shouldReturnFeatureOnManagementState() throws Exception {

        var featureName = "test_feature";
        var userId = "test_user_id";

        MvcResult result = mockMvc.perform(get(String.format("/validate/feature/%s", featureName))
                        .queryParam("user", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        assertThat(result
                .getResponse()
                .getStatus())
                .isEqualTo(200);

        var provided = getProvidedResponse(result.getResponse().getContentAsByteArray());

        var expected = Map.of(featureName, true);

        assertThat(provided).isEqualTo(expected);
    }

    @Profile("servlet")
    @SpringBootApplication
    static class TestServletContextApplication {

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

        public static void main(String[] args) {
            SpringApplication.run(FeatureLookupServletContextTests.TestServletContextApplication.class, args);
        }

    }

}
