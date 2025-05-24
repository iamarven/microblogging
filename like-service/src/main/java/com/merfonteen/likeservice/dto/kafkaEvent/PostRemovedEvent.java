package com.merfonteen.likeservice.dto.kafkaEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostRemovedEvent {
    private Long postId;
    private Long authorId;
}
