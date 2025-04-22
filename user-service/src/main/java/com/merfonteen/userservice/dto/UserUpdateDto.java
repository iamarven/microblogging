package com.merfonteen.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserUpdateDto {
    private String fullName;
    private String bio;
    private String profileImageUrl;
}
