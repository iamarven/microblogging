package com.merfonteen.userservice.mapper;

import com.merfonteen.userservice.dto.UserResponseDto;
import com.merfonteen.userservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toDto(User user);
    User toEntity(UserResponseDto userDto);
}
