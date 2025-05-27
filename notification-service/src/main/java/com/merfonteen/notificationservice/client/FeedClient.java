package com.merfonteen.notificationservice.client;

import com.merfonteen.notificationservice.dto.SubscriptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "feed-service", url = "${feed-service.url}")
public interface FeedClient {

    @GetMapping("/api/feed/subscribers/users/{id}")
    List<SubscriptionDto> getUserSubscribers(@PathVariable("id") Long id);
}
