package com.merfonteen.likeservice.service;

import com.merfonteen.likeservice.dto.LikePageResponseDto;

public interface LikeService {
    LikePageResponseDto getLikesForPost(Long postId, int page, int size);
}
