package com.merfonteen.likeservice.dto.kafkaEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LikeSentEvent {
    private Long likeId;
    private Long userId;
    private Long postId;
}
