package com.merfonteen.likeservice.service;

import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.likeservice.dto.LikeResponse;
import com.merfonteen.likeservice.dto.LikePageResponse;
import com.merfonteen.likeservice.dto.LikesSearchRequest;

public interface LikeService {

    LikePageResponse getLikesForPost(Long postId, LikesSearchRequest searchRequest);

    Long getLikeCount(Long postId);

    LikeResponse likePost(Long postId, Long currentUserId);

    LikeResponse removeLike(Long postId, Long currentUserId);

    void removeLikesOnPost(PostRemovedEvent event);
}
