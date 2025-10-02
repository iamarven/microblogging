package com.merfonteen.profileservice.client;

import com.merfonteen.dtos.PublicUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Component
public class UserClient {
    private final WebClient webClient;

    public PublicUserDto getUser(Long userId) {
        return webClient.get()
                .uri("/api/users/{id}", userId)
                .retrieve()
                .bodyToMono(PublicUserDto.class)
                .block();
    }
}
