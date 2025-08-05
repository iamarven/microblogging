package com.merfonteen.postservice.service;

import com.merfonteen.postservice.kafka.PostEventProducer;
import com.merfonteen.postservice.mapper.OutboxEventMapper;
import com.merfonteen.postservice.model.OutboxEvent;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.enums.OutboxEventType;
import com.merfonteen.postservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {
    private final OutboxEventMapper mapper;
    private final PostEventProducer postEventProducer;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void publishOutboxEvents() {
        PageRequest page = mapper.buildPageRequest(0, 100);
        Page<OutboxEvent> outboxEvents;

        do {
            outboxEvents = outboxEventRepository.findAllBySentFalse(page);
            process(outboxEvents);
            page = page.next();
        } while(!outboxEvents.isEmpty());
    }

    public void process(Page<OutboxEvent> outboxEvents) {
        for (OutboxEvent event : outboxEvents) {
            try {
                switch (event.getEventType()) {
                    case POST_CREATED -> postEventProducer.sendPostCreatedEvent(
                            mapper.mapPostCreatedEventFromJson(event.getPayload()));
                    case POST_REMOVED -> postEventProducer.sendPostRemovedEvent(
                            mapper.mapPostRemovedEventFromJson(event.getPayload()));
                    default -> {
                        log.warn("Unknown event type: {}", event.getEventType());
                        continue;
                    }
                }
                event.setSent(true);
            } catch (Exception e) {
                log.error("Failed to send Kafka event for outboxEventId={}: {}", event.getId(), e.getMessage());
            }
        }

        outboxEventRepository.saveAll(
                outboxEvents.stream().filter(OutboxEvent::isSent).toList()
        );
    }

    @Transactional
    public OutboxEvent create(Post post, OutboxEventType eventType) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Post")
                .aggregateId(post.getId())
                .eventType(eventType)
                .sent(false)
                .payload(mapper.mapToJson(post, eventType))
                .createdAt(Instant.now())
                .build();

        return outboxEventRepository.save(outboxEvent);
    }
}
