package com.merfonteen.profileservice.dto;

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

    @Min(value = 10)
    @Builder.Default
    private int limit = 10;

    @Builder.Default
    private boolean includeComments = true;

    private String cursor;
}
