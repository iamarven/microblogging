package com.merfonteen.feedservice.scheduler;

import com.merfonteen.feedservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutdatedFeedCleaner {
    private final FeedService feedService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldFeeds() {
        Instant nowMinusWeek = Instant.now().minus(7, ChronoUnit.DAYS);
        final int batchSize = 5_000;

        int total = 0, deleted;
        do {
            deleted = feedService.deleteFeedsBelowDate(nowMinusWeek, batchSize);
            total += deleted;
        } while (deleted == batchSize);

        log.info("Feed cleanup: deleted {} rows older than {}", total, nowMinusWeek);
    }
}
