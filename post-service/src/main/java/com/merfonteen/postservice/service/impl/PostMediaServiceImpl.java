package com.merfonteen.postservice.service.impl;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.postservice.client.MediaClient;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.PostMedia;
import com.merfonteen.postservice.repository.PostMediaRepository;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostCacheService;
import com.merfonteen.postservice.service.PostMediaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Objects;

@Primary
@RequiredArgsConstructor
@Service
public class PostMediaServiceImpl implements PostMediaService {

    private final MediaClient mediaClient;
    private final PostRepository postRepository;
    private final PostCacheService postCacheService;
    private final PostMediaRepository postMediaRepository;

    @Transactional
    @Override
    public FileUploadResponse uploadMediaToPost(Long postId, MultipartFile file, Long currentUserId) {
        Post post = findPostByIdOrThrowException(postId);

        if(!Objects.equals(post.getAuthorId(), currentUserId)) {
            throw new ForbiddenException("You cannot upload file to not your own post");
        }

        FileUploadResponse fileUploadResponse = mediaClient.uploadPostMedia(postId, file, currentUserId);

        PostMedia postMedia = PostMedia.builder()
                .post(post)
                .fileUrl(fileUploadResponse.getFileUrl())
                .createdAt(Instant.now())
                .build();

        postMediaRepository.save(postMedia);
        postCacheService.evictUserPostsCacheByUserId(currentUserId);

        return fileUploadResponse;
    }

    private Post findPostByIdOrThrowException(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found by id: " + postId));
    }
}
