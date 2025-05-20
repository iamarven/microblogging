package com.merfonteen.likeservice.service.impl;

import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.dto.LikePageResponseDto;
import com.merfonteen.likeservice.dto.kafkaEvent.LikeRemovedEvent;
import com.merfonteen.likeservice.dto.kafkaEvent.LikeSentEvent;
import com.merfonteen.likeservice.exception.BadRequestException;
import com.merfonteen.likeservice.exception.NotFoundException;
import com.merfonteen.likeservice.kafkaProducer.LikeEventProducer;
import com.merfonteen.likeservice.mapper.LikeMapper;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.LikeService;
import com.merfonteen.likeservice.util.LikeRateLimiter;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final LikeRateLimiter likeRateLimiter;
    private final LikeEventProducer likeEventProducer;
    private final StringRedisTemplate stringRedisTemplate;

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

    @Override
    public Long getLikeCount(Long postId) {
        String key = "like:count:post:" + postId;
        String cachedValue = stringRedisTemplate.opsForValue().get(key);

        if(cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        long countFromDb = likeRepository.countByPostId(postId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(countFromDb), Duration.ofMinutes(10));

        return countFromDb;
    }

    @Transactional
    @Override
    public LikeDto likePost(Long postId, Long currentUserId) {
        checkPostExistsOrThrowException(postId);

        Optional<Like> existing = likeRepository.findByPostIdAndUserId(postId, currentUserId);
        if(existing.isPresent()) {
            return likeMapper.toDto(existing.get());
        }

        likeRateLimiter.limitAmountOfLikes(currentUserId);

        Like newLike = Like.builder()
                .postId(postId)
                .userId(currentUserId)
                .createdAt(Instant.now())
                .build();

        likeRepository.save(newLike);
        log.info("New like with id '{}' was saved to database successfully", newLike.getId());

        stringRedisTemplate.opsForValue().increment("like:count:post:" + postId);

        LikeSentEvent likeSentEvent = LikeSentEvent.builder()
                .likeId(newLike.getId())
                .userId(currentUserId)
                .postId(postId)
                .build();

        likeEventProducer.sendLikeSentEvent(likeSentEvent);
        log.info("New message was sent to topic 'like-sent' successfully: {}", likeSentEvent);

        return likeMapper.toDto(newLike);
    }

    @Transactional
    @Override
    public LikeDto removeLike(Long postId, Long currentUserId) {
        checkPostExistsOrThrowException(postId);

        Optional<Like> likeToRemove = likeRepository.findByPostIdAndUserId(postId, currentUserId);
        likeRateLimiter.limitAmountOfUnlikes(currentUserId);

        if(likeToRemove.isEmpty()) {
            throw new BadRequestException("You did not like this post");
        }

        likeRepository.delete(likeToRemove.get());
        log.info("Like with id '{}' was removed", likeToRemove.get().getId());

        String cacheKey = "like:count:post:" + postId;
        if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey))) {
            stringRedisTemplate.opsForValue().decrement(cacheKey);
        }

        LikeRemovedEvent likeRemovedEvent = LikeRemovedEvent.builder()
                .likeId(likeToRemove.get().getId())
                .userId(currentUserId)
                .postId(postId)
                .build();

        likeEventProducer.sendLikeRemovedEvent(likeRemovedEvent);
        log.info("New message was sent to topic 'like-removed' successfully: {}", likeRemovedEvent);

        return likeMapper.toDto(likeToRemove.get());
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

