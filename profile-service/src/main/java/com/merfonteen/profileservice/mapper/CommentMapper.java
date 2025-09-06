package com.merfonteen.profileservice.mapper;

import com.merfonteen.profileservice.dto.CommentItemDto;
import com.merfonteen.profileservice.model.CommentReadModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentItemDto toDto(CommentReadModel commentReadModel);
}
