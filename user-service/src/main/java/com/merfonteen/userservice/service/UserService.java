package com.merfonteen.userservice.service;

import com.merfonteen.userservice.model.PrivateUserDto;
import com.merfonteen.userservice.model.PublicUserDto;
import com.merfonteen.userservice.model.UserCreateDto;
import com.merfonteen.userservice.model.UserUpdateDto;

public interface UserService {

    PrivateUserDto getCurrentUser(Long currentId);

    PublicUserDto getUserById(Long id);

    PublicUserDto createUser(UserCreateDto userCreateDto);

    PublicUserDto updateUser(Long id, UserUpdateDto userUpdateDto, Long currentUserId);

    PublicUserDto deleteUser(Long id, Long currentUserId);
}
