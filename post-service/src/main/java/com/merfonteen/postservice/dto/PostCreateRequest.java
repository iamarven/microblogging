package com.merfonteen.postservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostCreateRequest {
    @Size(max = 5000)
    @NotBlank(message = "Content is required")
    private String content;
    private String mediaUrl;
}
