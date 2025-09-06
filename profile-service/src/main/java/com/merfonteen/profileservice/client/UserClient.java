package com.merfonteen.profileservice.client;

import com.merfonteen.dtos.PublicUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class UserClient {
    private final WebClient webClient;

    @Bean
    public WebClient userWebClient(@Value("${url.user-service}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    public PublicUserDto getUser(Long userId) {
        return webClient.get()
                .uri("/api/users/{id}", userId)
                .retrieve()
                .bodyToMono(PublicUserDto.class)
                .block(Duration.ofMillis(300));
    }
}
