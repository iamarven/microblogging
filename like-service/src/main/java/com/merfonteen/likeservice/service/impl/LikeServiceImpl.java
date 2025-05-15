package com.merfonteen.likeservice.service.impl;

import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.dto.LikePageResponseDto;
import com.merfonteen.likeservice.dto.kafkaEvent.LikeSentEvent;
import com.merfonteen.likeservice.exception.NotFoundException;
import com.merfonteen.likeservice.kafkaProducer.LikeSentEventProducer;
import com.merfonteen.likeservice.mapper.LikeMapper;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.LikeService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class LikeServiceImpl implements LikeService {

    private final PostClient postClient;
    private final LikeMapper likeMapper;
    private final LikeRepository likeRepository;
    private final LikeSentEventProducer likeSentEventProducer;

    @Override
    public LikePageResponseDto getLikesForPost(Long postId, int page, int size) {
        checkPostExistsOrThrowException(postId);

        if(size > 100) {
            size = 100;
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Like> likesPage = likeRepository.findAllByPostId(postId, pageRequest);
        List<LikeDto> likesForPost = likeMapper.toDtos(likesPage.getContent());

        return LikePageResponseDto.builder()
                .likes(likesForPost)
                .currentPage(likesPage.getNumber())
                .totalPages(likesPage.getTotalPages())
                .totalElements(likesPage.getTotalElements())
                .isLastPage(likesPage.isLast())
                .build();
    }

    @Transactional
    @Override
    public LikeDto likePost(Long postId, Long currentUserId) {
        checkPostExistsOrThrowException(postId);

        Optional<Like> existing = likeRepository.findByPostIdAndUserId(postId, currentUserId);
        if(existing.isPresent()) {
            return likeMapper.toDto(existing.get());
        }

        Like newLike = Like.builder()
                .postId(postId)
                .userId(currentUserId)
                .createdAt(Instant.now())
                .build();

        likeRepository.save(newLike);
        log.info("New like with id '{}' was saved to database successfully", newLike.getId());

        LikeSentEvent sentEvent = LikeSentEvent.builder()
                .likeId(newLike.getId())
                .userId(currentUserId)
                .postId(postId)
                .build();

        likeSentEventProducer.sendLikeSentEvent(sentEvent);
        log.info("New message was sent to topic 'like-sent' successfully: {}", sentEvent);

        return likeMapper.toDto(newLike);
    }

    private void checkPostExistsOrThrowException(Long postId) {
        try {
            postClient.checkPostExists(postId);
        } catch (FeignException.NotFound e) {
            log.warn("Exception during interaction with post-client: post with id '{}' not found", postId);
            throw new NotFoundException(String.format("Post with id '%d' not found", postId));
        }
    }
}
