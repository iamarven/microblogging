package com.merfonteen.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentItemDto {
    private Long commentId;
    private Long postId;
    private Long authorId;
    private Instant createdAt;
    private String content;
    private long likesCount;
}
