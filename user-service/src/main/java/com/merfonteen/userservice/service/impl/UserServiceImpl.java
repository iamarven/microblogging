package com.merfonteen.userservice.service.impl;

import com.merfonteen.userservice.dto.UserResponseDto;
import com.merfonteen.userservice.exception.NotFoundException;
import com.merfonteen.userservice.mapper.UserMapper;
import com.merfonteen.userservice.model.User;
import com.merfonteen.userservice.repository.UserRepository;
import com.merfonteen.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id '%d' not found".formatted(id)));
        log.info("Successfully retrieved user: {}", user);
        return userMapper.toDto(user);
    }
}
