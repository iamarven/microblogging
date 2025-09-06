package com.merfonteen.profileservice.service;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.profileservice.repository.PostReadModelRepository;
import com.merfonteen.profileservice.repository.writers.PostProjectionWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostProjectionService {
    private final PostProjectionWriter postProjectionWriter;
    private final PostReadModelRepository postReadModelRepository;

    @Transactional
    public void applyPostCreated(PostCreatedEvent event) {
        postProjectionWriter.upsertPost(
                event.getPostId(),
                event.getAuthorId(),
                event.getCreatedAt(),
                event.getContent(),
                0L, 0L);
        log.info("New post='{}' was added to db post-read-model",  event.getPostId());
    }

    @Transactional
    public void applyPostRemoved(PostRemovedEvent event) {
        postReadModelRepository.deleteSilently(event.getPostId());
        log.info("post='{}' was removed from db post-read-model",  event.getPostId());
    }

    @Transactional
    public void incPostComments(Long postId, Long delta) {
        postProjectionWriter.incrementPostComments(postId, delta);
        log.info("number of comments on post='{}' was incremented", postId);
    }
}
