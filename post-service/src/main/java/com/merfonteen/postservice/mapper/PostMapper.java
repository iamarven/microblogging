package com.merfonteen.postservice.mapper;

import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.entity.Post;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface PostMapper {
    PostResponseDto toDto(Post post);
    List<PostResponseDto> toListDtos(List<Post> posts);
}
