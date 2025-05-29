package com.merfonteen.feedservice.kafka.eventProducer;

import com.merfonteen.feedservice.dto.event.SubscriptionCreatedEvent;
import com.merfonteen.feedservice.dto.event.SubscriptionRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubscriptionEventProducer {

    @Value("${topic.subscription-created}")
    private String subscriptionCreatedTopic;

    @Value("${topic.subscription-removed}")
    private String subscriptionRemovedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSubscriptionCreatedEvent(SubscriptionCreatedEvent event) {
        kafkaTemplate.send(subscriptionCreatedTopic, event.getSubscriptionId().toString(), event);
        log.info("SubscriptionCreatedEvent sent to topic '{}': {}", subscriptionCreatedTopic, event);
    }

    public void sendSubscriptionRemovedEvent(SubscriptionRemovedEvent event) {
        kafkaTemplate.send(subscriptionRemovedTopic, event.getTargetUserId().toString(), event);
        log.info("SubscriptionRemovedEvent sent to topic '{}': {}", subscriptionRemovedTopic, event);
    }
}
