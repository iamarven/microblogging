package com.merfonteen.postservice.mapper;

import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.entity.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface PostMapper {
    PostResponseDto toDto(Post post);
    Post toEntity(PostResponseDto postDto);
}
