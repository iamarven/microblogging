package com.merfonteen.feedservice.scheduler;

import com.merfonteen.feedservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OutboxEventScheduler {
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        outboxService.publishOutboxEvents();
    }
}
