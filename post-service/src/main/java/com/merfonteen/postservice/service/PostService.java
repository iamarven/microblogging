package com.merfonteen.postservice.service;

import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;

public interface PostService {
    PostResponseDto getPostById(Long id);
    UserPostsPageResponseDto getUserPosts(Long userId, int page, int size);
    PostResponseDto createPost(Long currentUserId, PostCreateDto createDto);
}
