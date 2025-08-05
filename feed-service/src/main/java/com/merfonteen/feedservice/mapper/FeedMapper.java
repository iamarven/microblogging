package com.merfonteen.feedservice.mapper;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.FeedPageResponse;
import com.merfonteen.feedservice.dto.FeedSearchRequest;
import com.merfonteen.feedservice.model.Feed;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FeedMapper {
    FeedDto toDto(Feed feed);

    List<FeedDto> toListDtos(List<Feed> feeds);

    default PageRequest buildPageRequest(FeedSearchRequest request) {
        return PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    default FeedPageResponse buildFeedPageResponse(List<FeedDto> feeds, Page<Feed> feedPage) {
        return FeedPageResponse.builder()
                .feeds(feeds)
                .currentPage(feedPage.getNumber())
                .totalElements(feedPage.getTotalElements())
                .totalPages(feedPage.getTotalPages())
                .isLastPage(feedPage.isLast())
                .build();
    }
}
