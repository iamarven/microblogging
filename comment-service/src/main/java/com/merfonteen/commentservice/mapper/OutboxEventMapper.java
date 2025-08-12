package com.merfonteen.commentservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.OutboxEventType;
import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OutboxEventMapper {
    private final ObjectMapper objectMapper;

    public String mapToJson(Comment comment, OutboxEventType eventType) {
        return writeValueAsString(comment, eventType);
    }

    public CommentCreatedEvent mapCommentCreatedEventFromJson(String json) {
        try {
            return objectMapper.readValue(json, CommentCreatedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PostCreatedEvent from JSON", e);
        }
    }

    public CommentRemovedEvent mapCommentRemovedEventFromJson(String json) {
        try {
            return objectMapper.readValue(json, CommentRemovedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PostCreatedEvent from JSON", e);
        }
    }

    public PageRequest buildPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    private String writeValueAsString(Comment comment, OutboxEventType eventType) {
        switch (eventType) {
            case COMMENT_CREATED -> {
                try {
                    return objectMapper.writeValueAsString(
                            new CommentCreatedEvent(comment.getId(), comment.getUserId(), comment.getPostId()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            case COMMENT_REMOVED -> {
                try {
                    return objectMapper.writeValueAsString(
                            new CommentRemovedEvent(comment.getId(), comment.getUserId(), comment.getPostId()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }
}