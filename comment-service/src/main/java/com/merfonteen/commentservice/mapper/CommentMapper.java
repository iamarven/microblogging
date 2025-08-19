package com.merfonteen.commentservice.mapper;

import com.merfonteen.commentservice.dto.CommentPageResponse;
import com.merfonteen.commentservice.dto.CommentResponse;
import com.merfonteen.commentservice.dto.CommentsOnPostSearchRequest;
import com.merfonteen.commentservice.dto.RepliesOnCommentSearchRequest;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentResponse toDto(Comment comment);

    Comment toEntity(CommentResponse responseDto);

    List<CommentResponse> toDtos(List<Comment> comments);

    default PageRequest buildPageRequest(CommentsOnPostSearchRequest searchRequest) {
        return PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.DESC, CommentSortField.from(searchRequest.getSortBy()).getFieldName())
        );
    }

    default PageRequest buildPageRequest(RepliesOnCommentSearchRequest searchRequest) {
        return PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    default CommentPageResponse buildCommentPageResponse(Page<Comment> pageComments, List<CommentResponse> commentDtos) {
        return CommentPageResponse.builder()
                .commentDtos(commentDtos)
                .currentPage(pageComments.getNumber())
                .totalPages(pageComments.getTotalPages())
                .totalElements(pageComments.getTotalElements())
                .isLastPage(pageComments.isLast())
                .build();
    }
}
