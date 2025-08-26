package com.merfonteen.likeservice.mapper;

import com.merfonteen.likeservice.dto.LikePageResponse;
import com.merfonteen.likeservice.dto.LikeResponse;
import com.merfonteen.likeservice.dto.LikesSearchRequest;
import com.merfonteen.likeservice.model.Like;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LikeMapper {

    LikeResponse toDto(Like like);

    List<LikeResponse> toDtos(List<Like> likes);

    default PageRequest buildPageRequest(LikesSearchRequest searchRequest) {
        return PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    default LikePageResponse buildLikePageResponse(List<LikeResponse> likesForPost, Page<Like> likesPage) {
        return LikePageResponse.builder()
                .likes(likesForPost)
                .currentPage(likesPage.getNumber())
                .totalPages(likesPage.getTotalPages())
                .totalElements(likesPage.getTotalElements())
                .isLastPage(likesPage.isLast())
                .build();
    }
}
