package com.merfonteen.commentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDto implements Serializable {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
