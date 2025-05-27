package com.merfonteen.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    @JsonProperty("follower_id")
    private Long followerId;
    @JsonProperty("followee_id")
    private Long followeeId;
    @JsonProperty("created_at")
    private Instant createdAt;
}