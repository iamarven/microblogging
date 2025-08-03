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
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {
    private final OutboxEventMapper mapper;
    private final PostEventProducer postEventProducer;
    private final OutboxEventRepository outboxEventRepository;


    public void publishOutboxEvents() {
        outboxEventRepository.findAllBySentFalse()
                .forEach(this::process);
    }

    @Transactional
    public void process(OutboxEvent event) {
        switch (event.getEventType()) {
            case POST_CREATED -> {
                postEventProducer.sendPostCreatedEvent(mapper.mapPostCreatedEventFromJson(event.getPayload()));
            }
            case POST_REMOVED -> {
                postEventProducer.sendPostRemovedEvent(mapper.mapPostRemovedEventFromJson(event.getPayload()));
            }
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
        event.setSent(true);
        outboxEventRepository.save(event);
    }

    public OutboxEvent create(Post post, OutboxEventType eventType) {
        return OutboxEvent.builder()
                .aggregateType("Post")
                .aggregateId(post.getId())
                .eventType(eventType)
                .sent(false)
                .payload(mapper.mapToJson(post, OutboxEventType.POST_CREATED))
                .createdAt(Instant.now())
                .build();
    }

    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        return outboxEventRepository.save(event);
    }
}
