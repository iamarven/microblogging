package com.merfonteen.likeservice.service.impl;

import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.dto.LikePageResponseDto;
import com.merfonteen.likeservice.dto.kafkaEvent.LikeRemovedEvent;
import com.merfonteen.likeservice.dto.kafkaEvent.LikeSentEvent;
import com.merfonteen.likeservice.exception.BadRequestException;
import com.merfonteen.likeservice.exception.NotFoundException;
import com.merfonteen.likeservice.kafka.eventProducer.LikeEventProducer;
import com.merfonteen.likeservice.mapper.LikeMapper;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.util.LikeRateLimiter;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {


    @Mock
    private PostClient postClient;

    @Mock
    private LikeMapper likeMapper;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private LikeRateLimiter likeRateLimiter;

    @Mock
    private LikeEventProducer likeEventProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LikeServiceImpl likeService;

    @Test
    void testGetLikesForPost_Success() {
        Long postId = 1L;
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Like like = Like.builder()
                .id(1L)
                .postId(postId)
                .userId(10L)
                .build();

        LikeDto likeDto = LikeDto.builder()
                .likeId(like.getId())
                .postId(postId)
                .userId(like.getUserId())
                .build();

        List<Like> likes = new ArrayList<>(List.of(like));
        List<LikeDto> likeDtos = new ArrayList<>(List.of(likeDto));

        LikePageResponseDto responseDto = LikePageResponseDto.builder()
                .likes(likeDtos)
                .totalPages(10)
                .totalElements(1L)
                .currentPage(0)
                .build();

        doNothing().when(postClient).checkPostExists(postId);
        when(likeRepository.findAllByPostId(postId, pageRequest)).thenReturn(new PageImpl<>(List.of(like)));
        when(likeMapper.toDtos(likes)).thenReturn(likeDtos);

        LikePageResponseDto result = likeService.getLikesForPost(postId, page, size);

        assertEquals(likeDto.getLikeId(), result.getLikes().get(0).getLikeId());
        assertEquals(responseDto.getTotalPages(), result.getTotalPages());
        assertEquals(responseDto.getTotalElements(), result.getTotalElements());
        assertEquals(responseDto.getCurrentPage(), result.getCurrentPage());
        verify(likeRepository, times(1)).findAllByPostId(postId, pageRequest);
    }

    @Test
    void testLikePost_Success() {
        Long postId = 1L;
        Long currentUserId = 50L;

        Like like = Like.builder()
                .id(1L)
                .userId(currentUserId)
                .postId(postId)
                .build();

        LikeDto likeDto = LikeDto.builder()
                .likeId(like.getId())
                .userId(currentUserId)
                .postId(postId)
                .build();

        doNothing().when(postClient).checkPostExists(postId);
        when(likeRepository.findByPostIdAndUserId(postId, currentUserId)).thenReturn(Optional.empty());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(likeRepository.save(any(Like.class))).thenReturn(like);
        when(likeMapper.toDto(any(Like.class))).thenReturn(likeDto);

        LikeDto result = likeService.likePost(postId, currentUserId);

        assertEquals(likeDto.getLikeId(), result.getLikeId());
        assertEquals(likeDto.getPostId(), result.getPostId());
        assertEquals(likeDto.getUserId(), result.getUserId());
        verify(likeRepository, times(1)).findByPostIdAndUserId(postId, currentUserId);
        verify(likeRateLimiter, times(1)).limitAmountOfLikes(currentUserId);
        verify(likeRepository, times(1)).save(any(Like.class));
        verify(likeEventProducer, times(1)).sendLikeSentEvent(any(LikeSentEvent.class));
    }

    @Test
    void testLikePost_WhenPostDoesNotExist_ShouldThrowException() {
        Long postId = 1L;

        doThrow(FeignException.NotFound.class)
                .when(postClient)
                .checkPostExists(postId);

        Exception exception = assertThrows(NotFoundException.class, () -> likeService.likePost(postId, 10L));
        assertEquals("Post with id '1' not found", exception.getMessage());
    }

    @Test
    void testRemoveLike_Success() {
        Long postId = 1L;
        Long currentUserId = 10L;

        Like likeToRemove = Like.builder()
                .id(1L)
                .postId(postId)
                .userId(currentUserId)
                .build();

        LikeDto likeDto = LikeDto.builder()
                .likeId(likeToRemove.getId())
                .postId(postId)
                .userId(currentUserId)
                .build();

        when(likeRepository.findByPostIdAndUserId(postId, currentUserId)).thenReturn(Optional.of(likeToRemove));
        when(likeMapper.toDto(any(Like.class))).thenReturn(likeDto);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LikeDto result = likeService.removeLike(postId, currentUserId);

        assertEquals(likeDto.getLikeId(), result.getLikeId());
        assertEquals(likeDto.getUserId(), result.getUserId());
        assertEquals(likeDto.getPostId(), result.getPostId());
        verify(likeRepository, times(1)).findByPostIdAndUserId(postId, currentUserId);
        verify(likeRateLimiter, times(1)).limitAmountOfUnlikes(currentUserId);
        verify(likeRepository, times(1)).delete(any(Like.class));
        verify(likeEventProducer, times(1)).sendLikeRemovedEvent(any(LikeRemovedEvent.class));
    }

    @Test
    void testRemoveLike_WhenPostDoesNotExist_ShouldThrowException() {
        Long postId = 1L;

        doThrow(FeignException.NotFound.class)
                .when(postClient)
                .checkPostExists(postId);

        Exception exception = assertThrows(NotFoundException.class, () -> likeService.removeLike(postId, 10L));
        assertEquals("Post with id '1' not found", exception.getMessage());
    }

    @Test
    void testRemoveLike_WhenUserDidNotLikeThePost_ShouldThrowException() {
        Long postId = 1L;
        Long currentUserId = 10L;

        doNothing().when(postClient).checkPostExists(postId);
        when(likeRepository.findByPostIdAndUserId(postId, currentUserId)).thenReturn((Optional.empty()));

        Exception exception = assertThrows(BadRequestException.class, () -> likeService.removeLike(postId, currentUserId));

        assertEquals("You did not like this post", exception.getMessage());
    }
}