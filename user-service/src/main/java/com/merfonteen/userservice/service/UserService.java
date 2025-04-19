package com.merfonteen.userservice.service;

import com.merfonteen.userservice.dto.UserResponseDto;

public interface UserService {
    UserResponseDto getUserById(Long id);
}
