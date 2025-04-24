package com.merfonteen.postservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostCreateDto {
    @NotBlank(message = "Content is required")
    private String content;
    private String mediaUrl;
}
