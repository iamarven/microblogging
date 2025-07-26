package com.merfonteen.postservice.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostsSearchRequest {
    @Builder.Default
    @Min(0)
    private int page = 0;
    @Min(1)
    @Builder.Default
    private int size = 10;
    @Builder.Default
    private String sortBy = "createdAt";
}
