package com.merfonteen.notificationservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LikeRemovedEvent {
    private Long likeId;
    private Long userId;
    private Long postId;
}
