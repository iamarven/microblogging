package com.merfonteen.likeservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikePageResponse;
import com.merfonteen.likeservice.dto.LikeResponse;
import com.merfonteen.likeservice.dto.LikesSearchRequest;
import com.merfonteen.likeservice.kafka.eventProducer.LikeEventProducer;
import com.merfonteen.likeservice.mapper.LikeMapper;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.impl.redis.LikeRateLimiter;
import com.merfonteen.likeservice.service.impl.redis.RedisCounter;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.merfonteen.likeservice.service.impl.LikeServiceImplTest.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
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
    private RedisCounter redisCounter;

    @InjectMocks
    private LikeServiceImpl likeService;

    @Test
    void testGetLikesForPost_Success() {
        PageRequest pageRequest = buildPageRequest();
        Like like = buildLikeEntity();
        LikeResponse likeResponse = buildLikeResponse(like);
        LikesSearchRequest searchRequest = buildLikesSearchRequest();

        List<LikeResponse> likeResponses = new ArrayList<>(List.of(likeResponse));
        Page<Like> likePage = new PageImpl<>(List.of(like));
        LikePageResponse responseDto = buildLikePageResponse(likeResponses, likePage);

        doNothing().when(postClient).checkPostExists(POST_ID);
        when(likeMapper.buildPageRequest(searchRequest)).thenReturn(pageRequest);
        when(likeRepository.findAllByPostId(POST_ID, pageRequest)).thenReturn(likePage);
        when(likeMapper.toDtos(likePage.getContent())).thenReturn(likeResponses);
        when(likeMapper.buildLikePageResponse(likeResponses, likePage)).thenReturn(responseDto);

        LikePageResponse result = likeService.getLikesForPost(POST_ID, searchRequest);

        assertThat(result).isEqualTo(responseDto);
        verify(likeRepository, times(1)).findAllByPostId(POST_ID, pageRequest);
    }

    @Test
    void testLikePost_Success() {
        Like like = buildLikeEntity();
        LikeResponse likeResponse = buildLikeResponse(like);

        doNothing().when(postClient).checkPostExists(POST_ID);
        when(likeRepository.findByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenReturn(like);
        when(likeMapper.toDto(any(Like.class))).thenReturn(likeResponse);

        LikeResponse result = likeService.likePost(POST_ID, USER_ID);

        assertThat(result).isEqualTo(likeResponse);
        verify(redisCounter).incrementCounter(POST_ID);
        verify(likeRepository, times(1)).findByPostIdAndUserId(POST_ID, USER_ID);
        verify(likeRateLimiter, times(1)).limitAmountOfLikes(USER_ID);
    }

    @Test
    void testLikePost_WhenPostDoesNotExist_ShouldThrowException() {
        doThrow(FeignException.NotFound.class)
                .when(postClient)
                .checkPostExists(POST_ID);

        Exception exception = assertThrows(NotFoundException.class, () -> likeService.likePost(POST_ID, USER_ID));
        assertEquals(String.format("Post with id '%d' not found", POST_ID), exception.getMessage());
    }

    @Test
    void testRemoveLike_Success() {
        Like likeToRemove = buildLikeEntity();
        LikeResponse likeResponse = buildLikeResponse(likeToRemove);

        when(likeRepository.findByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(Optional.of(likeToRemove));
        when(likeMapper.toDto(any(Like.class))).thenReturn(likeResponse);

        LikeResponse result = likeService.removeLike(POST_ID, USER_ID);

        assertThat(result).isEqualTo(likeResponse);
        verify(likeRepository, times(1)).findByPostIdAndUserId(POST_ID, USER_ID);
        verify(likeRateLimiter, times(1)).limitAmountOfUnlikes(USER_ID);
        verify(likeRepository, times(1)).delete(any(Like.class));
        verify(redisCounter).decrementCounter(POST_ID);
    }

    @Test
    void testRemoveLike_WhenPostDoesNotExist_ShouldThrowException() {
        doThrow(FeignException.NotFound.class)
                .when(postClient)
                .checkPostExists(POST_ID);

        Exception exception = assertThrows(NotFoundException.class, () -> likeService.removeLike(POST_ID, USER_ID));
        assertEquals(String.format("Post with id '%d' not found", POST_ID), exception.getMessage());
    }

    @Test
    void testRemoveLike_WhenUserDidNotLikeThePost_ShouldThrowException() {
        doNothing().when(postClient).checkPostExists(POST_ID);
        when(likeRepository.findByPostIdAndUserId(POST_ID, USER_ID)).thenReturn((Optional.empty()));

        Exception exception = assertThrows(BadRequestException.class, () -> likeService.removeLike(POST_ID, USER_ID));

        assertEquals("You did not like this post", exception.getMessage());
    }

    static class TestResources {
        static final Long LIKE_ID = 111L;
        static final Long USER_ID = 5L;
        static final Long POST_ID = 1L;
        static final int PAGE = 0;
        static final int SIZE = 10;

        static LikesSearchRequest buildLikesSearchRequest() {
            return LikesSearchRequest.builder().page(PAGE).size(SIZE).build();
        }

        static LikePageResponse buildLikePageResponse(List<LikeResponse> likesForPost, Page<Like> likesPage) {
            return LikePageResponse.builder()
                    .likes(likesForPost)
                    .currentPage(likesPage.getNumber())
                    .totalPages(likesPage.getTotalPages())
                    .totalElements(likesPage.getTotalElements())
                    .isLastPage(likesPage.isLast())
                    .build();
        }

        static Like buildLikeEntity() {
            return Like.builder()
                    .id(LIKE_ID)
                    .postId(POST_ID)
                    .userId(USER_ID)
                    .build();
        }

        static LikeResponse buildLikeResponse(Like like) {
            return LikeResponse.builder()
                    .id(like.getId())
                    .postId(like.getPostId())
                    .userId(like.getUserId())
                    .build();
        }

        static PageRequest buildPageRequest() {
            return PageRequest.of(PAGE, SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}