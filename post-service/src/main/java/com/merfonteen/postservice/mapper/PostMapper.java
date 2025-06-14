package com.merfonteen.postservice.mapper;

import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.PostMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface PostMapper {

    @Mapping(target = "mediaUrls", source = "media", qualifiedByName = "mapMediaToUrls")
    PostResponseDto toDto(Post post);

    List<PostResponseDto> toListDtos(List<Post> posts);

    @Named("mapMediaToUrls")
    default List<String> mapMediaToUrls(List<PostMedia> media) {
        if (media == null) return List.of();
        return media.stream()
                .map(PostMedia::getFileUrl)
                .toList();
    }
}
