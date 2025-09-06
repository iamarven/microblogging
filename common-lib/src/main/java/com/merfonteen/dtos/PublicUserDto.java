package com.merfonteen.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PublicUserDto {
    private Long id;
    private String username;
    private String fullName;
    private String bio;
    private String profileImageUrl;
    private Instant createdAt;
}
