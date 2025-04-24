package com.merfonteen.postservice.service;

import com.merfonteen.postservice.dto.PostResponseDto;

public interface PostService {
    PostResponseDto getPostById(Long id);
}
