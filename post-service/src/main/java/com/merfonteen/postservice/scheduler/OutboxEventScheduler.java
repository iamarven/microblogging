package com.merfonteen.postservice.scheduler;

import com.merfonteen.postservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxEventScheduler {
   private final OutboxService outboxService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 1)
    public void run() {
       outboxService.publishOutboxEvents();
    }
}
