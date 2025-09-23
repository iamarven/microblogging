package com.merfonteen.profileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class UserClientConfig {

    @Bean
    public WebClient userWebClient(@Value("${url.user-service}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
