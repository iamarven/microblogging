package com.merfonteen.postservice.service;

import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;

public interface PostService {
    PostResponseDto getPostById(Long id);
    PostResponseDto createPost(Long currentUserId, PostCreateDto createDto);
}
