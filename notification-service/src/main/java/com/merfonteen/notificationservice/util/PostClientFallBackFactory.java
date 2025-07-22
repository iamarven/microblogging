package com.merfonteen.notificationservice.util;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.notificationservice.client.PostClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostClientFallBackFactory implements FallbackFactory<PostClient> {

    @Override
    public PostClient create(Throwable cause) {
        return new PostClient() {
            @Override
            public Long getPostAuthorId(Long postId) {
                log.error("Fallback: Failed to get author by postId={}", postId);
                throw new BadRequestException("Notification service unavailable or returned error while getting.");
            }
        };
    }
}
