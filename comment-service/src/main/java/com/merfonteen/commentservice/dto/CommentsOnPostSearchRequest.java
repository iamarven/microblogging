package com.merfonteen.commentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentsOnPostSearchRequest {
    private Long postId;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
    private String sortBy;
}
