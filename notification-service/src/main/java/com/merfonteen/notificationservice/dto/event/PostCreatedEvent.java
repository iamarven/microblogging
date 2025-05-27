package com.merfonteen.notificationservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostCreatedEvent {
    private Long postId;
    private Long authorId;
    private Instant createdAt;
}
