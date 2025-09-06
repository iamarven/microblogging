package com.merfonteen.profileservice.mapper;

import com.merfonteen.profileservice.dto.PostItemDto;
import com.merfonteen.profileservice.model.PostReadModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostItemDto toDto(PostReadModel postReadModel);
}
