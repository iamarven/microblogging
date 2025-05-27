package com.merfonteen.notificationservice.kafkaListener;

import com.merfonteen.notificationservice.dto.event.SubscriptionCreatedEvent;
import com.merfonteen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubscriptionEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${topic.subscription-created}")
    public void handleSubscriptionCreatedEvent(SubscriptionCreatedEvent event, Acknowledgment ack) {
        log.info("Received subscription-created-event: {}", event);
        notificationService.sendFollowNotification(event.getFollowerId(), event.getFollowerId(), event.getSubscriptionId());
        ack.acknowledge();
    }
}
