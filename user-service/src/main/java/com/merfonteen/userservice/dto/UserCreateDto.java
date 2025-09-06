package com.merfonteen.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserCreateDto {
    @Size(max = 50)
    @NotBlank(message = "Username is required")
    private String username;
    @Size(max = 100)
    @Email(message = "Email is required")
    private String email;
    @Size(max = 100)
    private String fullName;
    private String bio;
}