package com.merfonteen.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PrivateUserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profileImageUrl;
    private Instant createdAt;
    private Boolean isActive;
}
