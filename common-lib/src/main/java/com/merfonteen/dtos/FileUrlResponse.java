package com.merfonteen.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUrlResponse {
    private String fileName;
    private String fileUrl;
    private Instant expiresAt;
}
