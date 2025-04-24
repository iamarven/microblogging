package com.merfonteen.userservice.service.impl;

import com.merfonteen.userservice.dto.PublicUserDto;
import com.merfonteen.userservice.dto.UserCreateDto;
import com.merfonteen.userservice.dto.UserUpdateDto;
import com.merfonteen.userservice.mapper.UserMapper;
import com.merfonteen.userservice.model.User;
import com.merfonteen.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testGetUserById_Success() {
        Long id = 1L;

        User user = User.builder()
                .id(1L)
                .username("testUsername")
                .isActive(true)
                .build();

        PublicUserDto expectedDto = PublicUserDto.builder()
                .id(1L)
                .username("testUsername")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.ofNullable(user));
        when(userMapper.toPublicDto(user)).thenReturn(expectedDto);

        PublicUserDto actualDto = userService.getUserById(id);

        assertEquals(expectedDto.getId(), actualDto.getId());
    }

    @Test
    void testCreateUser_Success() {
        UserCreateDto userCreateDto = UserCreateDto.builder()
                .username("testUsername")
                .email("test@gmail.com")
                .build();

        User user = User.builder()
                .id(1L)
                .username("testUsername")
                .email("test@gmail.com")
                .isActive(true)
                .build();

        PublicUserDto expectedDto = PublicUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();

        User savedUser = User.builder()
                .id(1L)
                .username("testUsername")
                .email("test@gmail.com")
                .isActive(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toPublicDto(any(User.class))).thenReturn(expectedDto);
        when(userRepository.findByEmailAndIsActiveTrue(userCreateDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsernameAndIsActiveTrue(userCreateDto.getUsername())).thenReturn(Optional.empty());

        PublicUserDto actualDto = userService.createUser(userCreateDto);

        assertEquals(expectedDto.getId(), actualDto.getId());
        assertEquals(expectedDto.getUsername(), actualDto.getUsername());
    }

    @Test
    void testUpdateUser_Success() {
        Long id = 1L;
        Long currentUserId = 1L;
        UserUpdateDto userUpdateDto = UserUpdateDto.builder()
                .bio("updatedBio")
                .build();

        User user = User.builder()
                .id(1L)
                .bio("testBio")
                .isActive(true)
                .build();

        PublicUserDto expectedDto = PublicUserDto.builder()
                .id(user.getId())
                .bio("updatedBio")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .bio("updatedBio")
                .isActive(true)
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toPublicDto(any(User.class))).thenReturn(expectedDto);

        PublicUserDto actual = userService.updateUser(id, userUpdateDto, currentUserId);

        assertEquals(expectedDto.getBio(), actual.getBio());
    }

    @Test
    void testDeleteUser_Success() {
        Long id = 1L;
        Long currentId = 1L;

        User user = User.builder()
                .id(1L)
                .isActive(true)
                .build();

        PublicUserDto expectedDto = PublicUserDto.builder()
                .id(user.getId())
                .build();

        User savedUser = User.builder()
                .id(1L)
                .bio("updatedBio")
                .isActive(true)
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toPublicDto(any(User.class))).thenReturn(expectedDto);

        PublicUserDto actual = userService.deleteUser(id, currentId);

        assertEquals(expectedDto.getId(), actual.getId());
    }
}