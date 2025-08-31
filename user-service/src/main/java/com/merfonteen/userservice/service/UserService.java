package com.merfonteen.userservice.service;

import com.merfonteen.userservice.dto.PrivateUserDto;
import com.merfonteen.userservice.dto.PublicUserDto;
import com.merfonteen.userservice.dto.UserCreateDto;
import com.merfonteen.userservice.dto.UserUpdateDto;

public interface UserService {

    PrivateUserDto getCurrentUser(Long currentId);

    PublicUserDto getUserById(Long id);

    PublicUserDto createUser(UserCreateDto userCreateDto);

    PublicUserDto updateUser(Long id, UserUpdateDto userUpdateDto, Long currentUserId);

    PublicUserDto deleteUser(Long id, Long currentUserId);
}
