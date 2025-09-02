package com.merfonteen.profileservice.service;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.profileservice.repository.CommentReadModelRepository;
import com.merfonteen.profileservice.repository.writers.CommentProjectionWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CommentProjectionService {
    private final CommentProjectionWriter commentProjectionWriter;
    private final CommentReadModelRepository commentReadModelRepository;

    @Transactional
    public void applyCommentCreated(CommentCreatedEvent event) {
        commentProjectionWriter.upsertComment(
                event.getCommentId(),
                event.getPostId(),
                event.getUserId(),
                event.getContent(),
                event.getCreatedAt(),
                0L
        );
    }

    @Transactional
    public void applyCommentRemoved(CommentRemovedEvent event) {
        commentReadModelRepository.deleteSilently(event.getCommentId());
    }
}
