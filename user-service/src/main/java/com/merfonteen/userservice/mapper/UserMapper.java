package com.merfonteen.userservice.mapper;

import com.merfonteen.userservice.model.PrivateUserDto;
import com.merfonteen.userservice.model.PublicUserDto;
import com.merfonteen.userservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    PrivateUserDto toPrivateDto(User user);
    PublicUserDto toPublicDto(User user);
}
