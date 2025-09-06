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
public class CommentsSearchRequest {

    @Min(value = 10)
    @Builder.Default
    private int limit = 10;

    private String cursor;
}
