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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Primary
@Slf4j
@RequiredArgsConstructor
@Service
public class PostMediaServiceImpl implements PostMediaService {

    private final MediaClient mediaClient;
    private final PostRepository postRepository;
    private final PostCacheService postCacheService;
    private final PostMediaRepository postMediaRepository;

    @Override
    public List<String> getMediaUrlsForPost(Long postId) {
        return postMediaRepository.findAllByPostId(postId)
                .stream()
                .map(PostMedia::getFileUrl)
                .toList();
    }

    @Transactional
    @Override
    public FileUploadResponse uploadMediaToPost(Long postId, MultipartFile file, Long currentUserId) {
        Post post = findPostByIdOrThrowException(postId);
        validatePostAuthor(currentUserId, post);

        FileUploadResponse fileUploadResponse = mediaClient.uploadPostMedia(postId, file, currentUserId);

        PostMedia postMedia = PostMedia.builder()
                .post(post)
                .fileName(fileUploadResponse.getFileName())
                .fileUrl(fileUploadResponse.getFileUrl())
                .createdAt(Instant.now())
                .build();

        postMediaRepository.save(postMedia);
        log.info("Post media file was saved successfully: {}", postMedia);

        postCacheService.evictUserPostsCacheByUserId(currentUserId);

        return fileUploadResponse;
    }

    @Transactional
    @Override
    public void deletePostMedia(Long postId, String fileType, String fileName, Long currentUserId) {
        Post post = findPostByIdOrThrowException(postId);
        validatePostAuthor(currentUserId, post);

        PostMedia postMediaFileToDelete = postMediaRepository.findByPostIdAndFileName(postId, fileName)
                .orElseThrow(() -> new NotFoundException(
                                String.format("There was not found post media file by post id '%d' and file name '%s'",
                                        postId, fileName))
                );

        mediaClient.deletePostMedia(fileType, fileName);
        postMediaRepository.delete(postMediaFileToDelete);
        log.info("Post media file was successfully deleted: '{}', '{}'", fileType, fileName);

        postCacheService.evictUserPostsCacheByUserId(currentUserId);
    }

    private Post findPostByIdOrThrowException(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found by id: " + postId));
    }

    private static void validatePostAuthor(Long currentUserId, Post post) {
        if (!Objects.equals(post.getAuthorId(), currentUserId)) {
            throw new ForbiddenException("You cannot upload file to not your own post");
        }
    }
}
