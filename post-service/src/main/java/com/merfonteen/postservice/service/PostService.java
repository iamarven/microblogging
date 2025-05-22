package com.merfonteen.postservice.service;

import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.model.enums.PostSortField;

public interface PostService {
    PostResponseDto getPostById(Long id);
    UserPostsPageResponseDto getUserPosts(Long userId, int page, int size, PostSortField postSortField);
    Long getPostCount(Long userId);
    PostResponseDto createPost(Long currentUserId, PostCreateDto createDto);
    PostResponseDto updatePost(Long id, PostUpdateDto updateDto, Long currentUserId);
    PostResponseDto deletePost(Long id, Long currentUserId);
}
