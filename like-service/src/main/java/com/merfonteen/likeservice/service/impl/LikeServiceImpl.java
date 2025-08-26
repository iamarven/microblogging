package com.merfonteen.likeservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.kafkaEvents.LikeRemovedEvent;
import com.merfonteen.kafkaEvents.LikeSentEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikePageResponse;
import com.merfonteen.likeservice.dto.LikeResponse;
import com.merfonteen.likeservice.dto.LikesSearchRequest;
import com.merfonteen.likeservice.kafka.eventProducer.LikeEventProducer;
import com.merfonteen.likeservice.mapper.LikeMapper;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.LikeService;
import com.merfonteen.likeservice.service.impl.redis.LikeRateLimiter;
import com.merfonteen.likeservice.service.impl.redis.RedisCounter;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final RedisCounter redisCounter;
    private final LikeRepository likeRepository;
    private final LikeRateLimiter likeRateLimiter;
    private final LikeEventProducer likeEventProducer;

    @Override
    public LikePageResponse getLikesForPost(Long postId, LikesSearchRequest searchRequest) {
        checkPostExistsOrThrowException(postId);

        PageRequest pageRequest = likeMapper.buildPageRequest(searchRequest);
        Page<Like> likesPage = likeRepository.findAllByPostId(postId, pageRequest);
        List<LikeResponse> likesForPost = likeMapper.toDtos(likesPage.getContent());

        return likeMapper.buildLikePageResponse(likesForPost, likesPage);
    }

    @Override
    public Long getLikeCount(Long postId) {
        String cachedValue = redisCounter.getCachedValue(postId);

        if (cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        long countFromDb = likeRepository.countByPostId(postId);
        redisCounter.setCounter(postId, countFromDb);

        return countFromDb;
    }

    @Transactional
    @Override
    public LikeResponse likePost(Long postId, Long currentUserId) {
        checkPostExistsOrThrowException(postId);

        Optional<Like> existing = likeRepository.findByPostIdAndUserId(postId, currentUserId);
        if (existing.isPresent()) {
            return likeMapper.toDto(existing.get());
        }

        likeRateLimiter.limitAmountOfLikes(currentUserId);

        Like newLike = Like.builder()
                .postId(postId)
                .userId(currentUserId)
                .createdAt(Instant.now())
                .build();

        Like savedLike = likeRepository.save(newLike);
        log.info("New like with id '{}' was saved successfully", savedLike.getId());

        redisCounter.incrementCounter(postId);

        LikeSentEvent likeSentEvent = LikeSentEvent.builder()
                .likeId(newLike.getId())
                .userId(currentUserId)
                .postId(postId)
                .build();

        likeEventProducer.sendLikeSentEvent(likeSentEvent);

        return likeMapper.toDto(savedLike);
    }

    @Transactional
    @Override
    public LikeResponse removeLike(Long postId, Long currentUserId) {
        checkPostExistsOrThrowException(postId);
        likeRateLimiter.limitAmountOfUnlikes(currentUserId);

        Optional<Like> likeToRemove = likeRepository.findByPostIdAndUserId(postId, currentUserId);

        if (likeToRemove.isEmpty()) {
            throw new BadRequestException("You did not like this post");
        }

        likeRepository.delete(likeToRemove.get());
        log.info("Like with id '{}' was removed", likeToRemove.get().getId());

        redisCounter.decrementCounter(postId);

        LikeRemovedEvent likeRemovedEvent = LikeRemovedEvent.builder()
                .likeId(likeToRemove.get().getId())
                .userId(currentUserId)
                .postId(postId)
                .build();

        likeEventProducer.sendLikeRemovedEvent(likeRemovedEvent);
        log.info("New message was sent to topic 'like-removed' successfully: {}", likeRemovedEvent);

        return likeMapper.toDto(likeToRemove.get());
    }

    @Transactional
    @Override
    public void removeLikesOnPost(PostRemovedEvent event) {
        int deleted = likeRepository.deleteAllByPostId(event.getPostId());
        log.info("Deleted {} likes for postId={}", deleted, event.getPostId());
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

