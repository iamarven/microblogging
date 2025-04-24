package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.entity.Post;
import com.merfonteen.postservice.exception.NotFoundException;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Primary
@RequiredArgsConstructor
@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final UserClient userClient;

    @Override
    public PostResponseDto getPostById(Long id) {
        Post post = findPostByIdOrThrowException(id);
        log.info("Getting post with id '{}'", id);
        return postMapper.toDto(post);
    }

    @Transactional
    @Override
    public PostResponseDto createPost(Long currentUserId, PostCreateDto createDto) {
        try {
            userClient.checkUserExists(currentUserId);
        } catch (FeignException.NotFound e) {
            log.error("During creation post user with id '{}' was not found", currentUserId);
            throw new NotFoundException(String.format("User with id '%d' not found", currentUserId));
        }

        Post post = Post.builder()
                .authorId(currentUserId)
                .content(createDto.getContent())
                .mediaUrl(createDto.getMediaUrl())
                .createdAt(Instant.now())
                .build();

        postRepository.save(post);
        log.info("Post with id '{}' successfully created by user '{}'", post.getId(), currentUserId);

        return postMapper.toDto(post);
    }

    private Post findPostByIdOrThrowException(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Post with id '%d' not found", id)));
    }
}
