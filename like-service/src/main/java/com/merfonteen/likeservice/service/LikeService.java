package com.merfonteen.likeservice.service;

import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.dto.LikePageResponseDto;

public interface LikeService {
    LikePageResponseDto getLikesForPost(Long postId, int page, int size);
    Long getLikeCount(Long postId);
    LikeDto likePost(Long postId, Long currentUserId);
    LikeDto removeLike(Long postId, Long currentUserId);
}
