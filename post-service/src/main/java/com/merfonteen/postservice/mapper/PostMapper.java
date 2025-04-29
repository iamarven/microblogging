package com.merfonteen.postservice.mapper;

import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.model.Post;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface PostMapper {
    PostResponseDto toDto(Post post);
    List<PostResponseDto> toListDtos(List<Post> posts);
}
