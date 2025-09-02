package com.merfonteen.postservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OutboxEventMapper {
    private final ObjectMapper objectMapper;

    public String mapToJson(Post post, OutboxEventType eventType) {
        return writeValueAsString(post, eventType);
    }

    public PostCreatedEvent mapPostCreatedEventFromJson(String json) {
        try {
            return objectMapper.readValue(json, PostCreatedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PostCreatedEvent from JSON", e);
        }
    }

    public PostRemovedEvent mapPostRemovedEventFromJson(String json) {
        try {
            return objectMapper.readValue(json, PostRemovedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PostCreatedEvent from JSON", e);
        }
    }

    public PageRequest buildPageRequest(int page, int size) {
        return  PageRequest.of(page, size, Sort.by("createdAt").ascending());
    }

    private String writeValueAsString(Post post, OutboxEventType eventType) {
        switch (eventType) {
            case POST_CREATED -> {
                try {
                    return objectMapper.writeValueAsString(
                            new PostCreatedEvent(post.getId(), post.getAuthorId(), post.getContent(), post.getCreatedAt()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            case POST_REMOVED -> {
                try {
                    return objectMapper.writeValueAsString(
                            new PostRemovedEvent(post.getId(), post.getAuthorId()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }
}
