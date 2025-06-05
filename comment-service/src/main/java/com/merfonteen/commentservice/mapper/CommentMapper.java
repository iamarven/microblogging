package com.merfonteen.commentservice.mapper;

import com.merfonteen.commentservice.dto.CommentResponseDto;
import com.merfonteen.commentservice.model.Comment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface CommentMapper {
    CommentResponseDto toDto(Comment comment);
    Comment toEntity(CommentResponseDto responseDto);
    List<CommentResponseDto> toDtos(List<Comment> comments);
}
