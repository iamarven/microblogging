package com.merfonteen.commentservice.service;

import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.mapper.OutboxEventMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.OutboxEvent;
import com.merfonteen.commentservice.model.enums.OutboxEventType;
import com.merfonteen.commentservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {
    private final OutboxEventMapper mapper;
    private final OutboxEventRepository repository;
    private final CommentEventProducer commentEventProducer;

    @Transactional
    public void publishOutboxEvents() {
        PageRequest page = mapper.buildPageRequest(0, 100);
        Page<OutboxEvent> outboxEvents;
        do {
            outboxEvents = repository.findAllBySentFalse(page);
            process(outboxEvents);
            page = page.next();
        } while(!outboxEvents.isEmpty());
    }

    public void process(Page<OutboxEvent> outboxEvents) {
        for (OutboxEvent event : outboxEvents) {
            try {
                switch (event.getEventType()) {
                    case COMMENT_CREATED -> commentEventProducer.sendCommentCreatedEvent(
                            mapper.mapCommentCreatedEventFromJson(event.getPayload()));
                    case COMMENT_REMOVED -> commentEventProducer.sendCommentRemovedEvent(
                            mapper.mapCommentRemovedEventFromJson(event.getPayload()));
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

        repository.saveAll(
                outboxEvents.stream().filter(OutboxEvent::isSent).toList()
        );
    }

    @Transactional
    public OutboxEvent create(Comment comment, OutboxEventType eventType) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Comment")
                .aggregateId(comment.getId())
                .eventType(eventType)
                .sent(false)
                .payload(mapper.mapToJson(comment, eventType))
                .createdAt(Instant.now())
                .build();

        return repository.save(outboxEvent);
    }
}
