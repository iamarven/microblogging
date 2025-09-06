package com.merfonteen.userservice.service.impl;

import com.merfonteen.dtos.PublicUserDto;
import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

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
    void testGetUserById_WhenUserNotFound_ShouldThrowException() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserById(id));
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
    void testCreateUser_WhenEmailAlreadyExists_ShouldThrowException() {
        UserCreateDto dto = UserCreateDto.builder()
                .username("testUsername")
                .email("test@gmail.com")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .username("testUsername")
                .email("test@gmail.com")
                .isActive(true)
                .build();

        when(userRepository.findByEmailAndIsActiveTrue(dto.getEmail())).thenReturn(Optional.ofNullable(existingUser));

        Exception exception = assertThrows(BadRequestException.class, () -> userService.createUser(dto));
        assertEquals(exception.getMessage(), "User with email 'test@gmail.com' already exists");
    }

    @Test
    void testCreateUser_WhenUsernameAlreadyExists_ShouldThrowException() {
        UserCreateDto dto = UserCreateDto.builder()
                .username("testUsername")
                .email("newEmail@gmail.com")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .username("testUsername")
                .email("test@gmail.com")
                .isActive(true)
                .build();

        when(userRepository.findByUsernameAndIsActiveTrue(dto.getUsername())).thenReturn(Optional.ofNullable(existingUser));

        Exception exception = assertThrows(BadRequestException.class, () -> userService.createUser(dto));
        assertEquals(exception.getMessage(), "User with username 'testUsername' already exists");
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
    void testUpdateUser_WhenUserNotFound_ShouldThrowException() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.updateUser(id, new UserUpdateDto(), 100L));
    }

    @Test
    void testUpdateUser_WhenNotAllowed_ShouldThrowException() {
        Long id = 1L;
        UserUpdateDto dto = new UserUpdateDto();
        Long currentId = 10L;

        User user = User.builder()
                .id(id)
                .isActive(true)
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.ofNullable(user));

        Exception exception = assertThrows(ForbiddenException.class, () -> userService.updateUser(id, dto, currentId));
        assertEquals(exception.getMessage(), "You are not allowed to modify this user");
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

    @Test
    void testDeleteUser_WhenUserNotFound_ShouldThrowException() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.deleteUser(id, 100L));
    }

    @Test
    void testDeleteUser_WhenNotAllowed_ShouldThrowException() {
        Long id = 1L;
        Long currentId = 10L;

        User user = User.builder()
                .id(id)
                .isActive(true)
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.ofNullable(user));

        Exception exception = assertThrows(ForbiddenException.class, () -> userService.deleteUser(id, currentId));
        assertEquals(exception.getMessage(), "You are not allowed to modify this user");
    }
}