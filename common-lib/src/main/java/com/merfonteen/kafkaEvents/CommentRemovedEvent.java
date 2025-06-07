package com.merfonteen.kafkaEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentRemovedEvent {
    private Long commentId;
    private Long userId;
    private Long postId;
}
