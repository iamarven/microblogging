package com.merfonteen.userservice.mapper;

import com.merfonteen.dtos.PublicUserDto;
import com.merfonteen.userservice.dto.PrivateUserDto;
import com.merfonteen.userservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    PrivateUserDto toPrivateDto(User user);
    PublicUserDto toPublicDto(User user);
    User toEntity(PrivateUserDto userDto);
}
