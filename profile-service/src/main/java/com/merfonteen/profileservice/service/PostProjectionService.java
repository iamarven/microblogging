package com.merfonteen.profileservice.service;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.profileservice.repository.PostReadModelRepository;
import com.merfonteen.profileservice.repository.writers.PostProjectionWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    }

    @Transactional
    public void applyPostRemoved(PostRemovedEvent event) {
        postReadModelRepository.deleteSilently(event.getPostId());
    }
}
