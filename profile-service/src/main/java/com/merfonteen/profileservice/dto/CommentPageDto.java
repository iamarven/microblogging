package com.merfonteen.profileservice.dto;

import java.util.List;

public record CommentPageDto(List<CommentItemDto> comments, String nextCursor) {
}
