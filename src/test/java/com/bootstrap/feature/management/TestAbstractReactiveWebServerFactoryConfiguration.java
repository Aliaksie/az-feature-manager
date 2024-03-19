package com.bootstrap.feature.management;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.reactive.HttpHandler;

@Profile("web-flux")
@Configuration
public class TestAbstractReactiveWebServerFactoryConfiguration {

    @Bean
    public AbstractReactiveWebServerFactory reactiveWebServerFactory() {
        return new AbstractReactiveWebServerFactory() {

            @Override
            public WebServer getWebServer(HttpHandler httpHandler) {
                return null;
            }

        };
    }

}
