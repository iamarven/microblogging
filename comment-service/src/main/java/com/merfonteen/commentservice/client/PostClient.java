package com.merfonteen.commentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", url = "${post-service.url}")
public interface PostClient {

    @GetMapping("/api/posts/{id}")
    void checkPostExists(@PathVariable("id") Long postId);
}
