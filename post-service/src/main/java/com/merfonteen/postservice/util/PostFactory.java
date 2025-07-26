package com.merfonteen.postservice.util;

import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.model.Post;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PostFactory {

    public Post create(Long userId, PostCreateRequest request) {
        return Post.builder()
                .authorId(userId)
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();
    }
}
