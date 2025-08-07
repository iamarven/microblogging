package com.merfonteen.feedservice.service;

import com.merfonteen.feedservice.kafka.eventProducer.SubscriptionEventProducer;
import com.merfonteen.feedservice.mapper.OutboxEventMapper;
import com.merfonteen.feedservice.model.OutboxEvent;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.model.enums.OutboxEventType;
import com.merfonteen.feedservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {
    private final OutboxEventMapper mapper;
    private final SubscriptionEventProducer eventProducer;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void publishOutboxEvents() {
        PageRequest pageRequest = mapper.buildPageRequest(0, 100);
        Page<OutboxEvent> outboxEvents;

        do {
            outboxEvents = outboxEventRepository.findAllBySentFalse(pageRequest);
            process(outboxEvents);
            pageRequest.next();
        }
        while (!outboxEvents.isEmpty());
    }

    public void process(Page<OutboxEvent> outboxEvents) {
        for (OutboxEvent event : outboxEvents) {
            try {
                switch (event.getEventType()) {
                    case SUBSCRIPTION_CREATED -> {
                        eventProducer.sendSubscriptionCreatedEvent(
                                mapper.mapSubscriptionCreatedEventFromJson(event.getPayload())
                        );
                    }
                    case SUBSCRIPTION_REMOVED -> {
                        eventProducer.sendSubscriptionRemovedEvent(
                                mapper.mapSubscriptionRemovedEventFromJson(event.getPayload())
                        );
                    }
                    default -> {
                        log.warn("Unknown event type {}", event.getEventType());
                        continue;
                    }
                }
                event.setSent(true);
            } catch (Exception e) {
                log.error("Failed to send Kafka event for outboxEventId={}: {}", event.getId(), e.getMessage());
            }
        }
        outboxEventRepository.saveAll(outboxEvents.stream().filter(OutboxEvent::isSent).toList());
    }

    public OutboxEvent create(Subscription subscription, Long currentUserId, Long targetUserId, OutboxEventType type) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Subscription")
                .aggregateId(subscription.getId())
                .eventType(type)
                .payload(mapper.mapToJson(subscription, currentUserId, targetUserId, type))
                .sent(false)
                .createdAt(subscription.getCreatedAt())
                .build();

        return outboxEventRepository.save(outboxEvent);
    }
}
