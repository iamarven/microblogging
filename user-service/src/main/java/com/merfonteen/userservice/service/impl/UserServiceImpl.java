package com.merfonteen.userservice.service.impl;

import com.merfonteen.userservice.dto.PublicUserDto;
import com.merfonteen.userservice.dto.UserCreateDto;
import com.merfonteen.userservice.dto.PrivateUserDto;
import com.merfonteen.userservice.dto.UserUpdateDto;
import com.merfonteen.userservice.exception.BadRequestException;
import com.merfonteen.userservice.exception.NotFoundException;
import com.merfonteen.userservice.mapper.UserMapper;
import com.merfonteen.userservice.model.User;
import com.merfonteen.userservice.repository.UserRepository;
import com.merfonteen.userservice.service.UserService;
import com.merfonteen.userservice.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Primary
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    public PrivateUserDto getCurrentUser(Long currentId) {
        return userMapper.toPrivateDto(findUserByIdOrThrowException(currentId));
    }

    @Override
    public PublicUserDto getUserById(Long id) {
        User user = findUserByIdOrThrowException(id);
        log.info("Successfully retrieved user: {}", user);
        return userMapper.toPublicDto(user);
    }

    @Override
    public PublicUserDto createUser(UserCreateDto userCreateDto) {
        validEmailAndUsername(userCreateDto);

        User.UserBuilder userBuilder = User.builder()
                .username(userCreateDto.getUsername())
                .email(userCreateDto.getEmail())
                .createdAt(Instant.now())
                .isActive(true);

        Optional.ofNullable(userCreateDto.getFullName()).ifPresent(userBuilder::fullName);
        Optional.ofNullable(userCreateDto.getBio()).ifPresent(userBuilder::bio);

        User user = userBuilder.build();

        userRepository.save(user);
        log.info("Successfully saved to database user: {}", user);

        return userMapper.toPublicDto(user);
    }

    @Override
    public PublicUserDto updateUser(Long id, UserUpdateDto userUpdateDto, Long currentUserId) {
        User user = findUserByIdOrThrowException(id);

        AuthUtil.requireSelfAccess(id, currentUserId);

        Optional.ofNullable(userUpdateDto.getFullName()).ifPresent(user::setFullName);
        Optional.ofNullable(userUpdateDto.getBio()).ifPresent(user::setBio);
        Optional.ofNullable(userUpdateDto.getProfileImageUrl()).ifPresent(user::setProfileImageUrl);

        userRepository.save(user);
        log.info("User with id '{}' successfully updated", user.getId());

        return userMapper.toPublicDto(user);
    }

    @Override
    public PublicUserDto deleteUser(Long id, Long currentUserId) {
        User user = findUserByIdOrThrowException(id);

        AuthUtil.requireSelfAccess(id, currentUserId);

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User with id '{}' marked as deactivated", id);
        return userMapper.toPublicDto(user);
    }

    private User findUserByIdOrThrowException(Long id) {
        return userRepository.findById(id)
                .filter(User::getIsActive)
                .orElseThrow(() -> new NotFoundException(String.format("User with id '%d' not found", id)));
    }

    private void validEmailAndUsername(UserCreateDto dto) {
        if(userRepository.findByEmailAndIsActiveTrue(dto.getEmail()).isPresent()) {
            log.warn("User with email '{}' already exists", dto.getEmail());
            throw new BadRequestException(String.format("User with email '%s' already exists", dto.getEmail()));
        }
        if(userRepository.findByUsernameAndIsActiveTrue(dto.getUsername()).isPresent()) {
            log.warn("User with username '{}' already exists", dto.getUsername());
            throw new BadRequestException(String.format("User with username '%s' already exists", dto.getUsername()));
        }
    }
}
