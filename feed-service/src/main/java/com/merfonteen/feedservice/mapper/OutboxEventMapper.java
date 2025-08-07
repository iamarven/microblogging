package com.merfonteen.feedservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.model.enums.OutboxEventType;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.kafkaEvents.SubscriptionCreatedEvent;
import com.merfonteen.kafkaEvents.SubscriptionRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OutboxEventMapper {
    private final ObjectMapper objectMapper;

    public String mapToJson(Subscription subscription,
                            Long currentUserId,
                            Long targetUserId,
                            OutboxEventType eventType) {
        return writeValueAsString(subscription, currentUserId, targetUserId, eventType);
    }

    public SubscriptionCreatedEvent mapSubscriptionCreatedEventFromJson(String json) {
        try {
            return objectMapper.readValue(json, SubscriptionCreatedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PostCreatedEvent from JSON", e);
        }
    }

    public SubscriptionRemovedEvent mapSubscriptionRemovedEventFromJson(String json) {
        try {
            return objectMapper.readValue(json, SubscriptionRemovedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse PostCreatedEvent from JSON", e);
        }
    }

    public PageRequest buildPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String writeValueAsString(Subscription subscription,
                                      Long currentUserId,
                                      Long targetUserId,
                                      OutboxEventType eventType) {
        switch (eventType) {
            case SUBSCRIPTION_CREATED -> {
                try {
                    return objectMapper.writeValueAsString(
                            new SubscriptionCreatedEvent(subscription.getId(), currentUserId, targetUserId));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            case SUBSCRIPTION_REMOVED -> {
                try {
                    return objectMapper.writeValueAsString(
                            new SubscriptionRemovedEvent(targetUserId));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }
}
