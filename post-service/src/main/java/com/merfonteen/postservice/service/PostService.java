package com.merfonteen.postservice.service;

import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostUpdateRequest;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;

public interface PostService {
    PostResponse getPostById(Long id);

    Long getPostAuthorId(Long postId);

    UserPostsPageResponse getUserPosts(Long userId, PostsSearchRequest request);

    Long getPostCount(Long userId);

    PostResponse createPost(Long currentUserId, PostCreateRequest createDto);

    PostResponse updatePost(Long id, PostUpdateRequest updateDto, Long currentUserId);

    void deletePost(Long id, Long currentUserId);
}
