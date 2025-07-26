package com.merfonteen.postservice.mapper;

import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.PostMedia;
import com.merfonteen.postservice.model.enums.PostSortField;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "mediaUrls", source = "media", qualifiedByName = "mapMediaToUrls")
    PostResponse toDto(Post post);

    List<PostResponse> toListDtos(List<Post> posts);

    @Named("mapMediaToUrls")
    default List<String> mapMediaToUrls(List<PostMedia> media) {
        if (media == null) return List.of();
        return media.stream()
                .map(PostMedia::getFileUrl)
                .toList();
    }

    default Pageable buildPageable(PostsSearchRequest request) {
        return PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, PostSortField.from(request.getSortBy()).getFieldName())
        );
    }

    default UserPostsPageResponse buildUserPostsPageResponse(List<PostResponse> posts, Page<Post> page) {
        return UserPostsPageResponse.builder()
                .posts(posts)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .isLastPage(page.isLast())
                .build();
    }
}
