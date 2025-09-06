package com.merfonteen.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostItemDto {
    private Long postId;
    private Long authorId;
    private Instant createdAt;
    private String content;
    @Builder.Default
    private List<CommentItemDto> comments = new ArrayList<>();
    private long likesCount;
    private long commentsCount;
}
