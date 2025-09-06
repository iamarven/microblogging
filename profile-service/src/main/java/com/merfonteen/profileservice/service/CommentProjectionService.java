package com.merfonteen.profileservice.service;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.profileservice.repository.CommentReadModelRepository;
import com.merfonteen.profileservice.repository.writers.CommentProjectionWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentProjectionService {
    private final CommentProjectionWriter commentProjectionWriter;
    private final CommentReadModelRepository commentReadModelRepository;
    private final PostProjectionService postProjectionService;

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
        log.info("New comment='{}' was added to db comment-read-model", event.getCommentId());

        postProjectionService.incPostComments(event.getPostId(), 1L);
    }

    @Transactional
    public void applyCommentRemoved(CommentRemovedEvent event) {
        commentReadModelRepository.deleteSilently(event.getCommentId());
        log.info("comment='{}' was removed from db comment-read-model", event.getCommentId());

        postProjectionService.incPostComments(event.getPostId(), -1L);
    }
}
