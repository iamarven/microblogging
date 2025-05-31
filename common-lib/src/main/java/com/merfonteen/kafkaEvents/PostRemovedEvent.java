package com.merfonteen.kafkaEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRemovedEvent {
    private Long postId;
    private Long authorId;
}
