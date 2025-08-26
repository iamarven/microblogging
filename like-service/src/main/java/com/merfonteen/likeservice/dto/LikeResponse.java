package com.merfonteen.likeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeResponse {
    private Long id;
    private Long userId;
    private Long postId;
    private Instant createdAt;
}
