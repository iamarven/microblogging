package com.merfonteen.likeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikesSearchRequest {
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
}
