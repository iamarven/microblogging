package com.merfonteen.notificationservice.client;

import com.merfonteen.notificationservice.util.PostClientFallBackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "post-service",
        url = "${post-service.url}",
        fallbackFactory = PostClientFallBackFactory.class
)
public interface PostClient {

    @GetMapping("/api/posts/{id}/author-id")
    Long getPostAuthorId(@PathVariable("id") Long postId);
}
