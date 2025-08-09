package com.merfonteen.commentservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentReplyRequest {
    @NotNull
    @Positive
    private Long parentId;

    @NotBlank
    @Size(min = 1, max = 5000)
    private String content;
}
