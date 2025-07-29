package com.merfonteen.postservice.kafka;

import com.merfonteen.postservice.mapper.OutboxEventMapper;
import com.merfonteen.postservice.model.OutboxEvent;
import com.merfonteen.postservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxEventProcessor {
    private final OutboxEventMapper mapper;
    private final PostEventProducer postEventProducer;
    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAllBySentFalse();
        for (OutboxEvent event : outboxEvents) {
            try {
                switch (event.getEventType()) {
                    case POST_CREATED -> {
                        postEventProducer.sendPostCreatedEvent(mapper.mapPostCreatedEventFromJson(event.getPayload()));
                        event.setSent(true);
                    }
                    case POST_REMOVED -> {
                        postEventProducer.sendPostRemovedEvent(mapper.mapPostRemovedEventFromJson(event.getPayload()));
                        event.setSent(true);
                    }
                    default -> log.warn("Unknown event type: {}", event.getEventType());
                }
            } catch (Exception ex) {
                log.error("Failed to process outbox event with id {}: {}", event.getId(), ex.getMessage(), ex);
            }
        }
        outboxEventRepository.saveAll(outboxEvents);
    }
}
