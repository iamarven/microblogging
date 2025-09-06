package com.merfonteen.profileservice.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSearchRequest {

    @Builder.Default
    private boolean includeBasic = true;

    @Min(value = 0)
    @Builder.Default
    private int postsLimit = 10;

    private String postsCursor;
}
