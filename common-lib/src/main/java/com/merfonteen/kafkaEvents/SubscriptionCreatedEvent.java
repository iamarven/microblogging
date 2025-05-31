package com.merfonteen.kafkaEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SubscriptionCreatedEvent {
    private Long subscriptionId;
    private Long followerId;
    private Long followeeId;
}
