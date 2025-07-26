package com.merfonteen.postservice.util;

import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.postservice.client.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostValidator {
    private final UserClient userClient;

    public void checkUserExists(Long userId) {
        try {
            userClient.checkUserExists(userId);
        } catch (FeignException.NotFound e) {
            log.error("User with id '{}' not found", userId);
            throw new NotFoundException(String.format("User with id '%d' not found", userId));
        }
    }
}
