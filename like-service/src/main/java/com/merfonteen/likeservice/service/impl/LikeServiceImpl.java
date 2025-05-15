package com.merfonteen.likeservice.service.impl;

import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.dto.LikePageResponseDto;
import com.merfonteen.likeservice.exception.NotFoundException;
import com.merfonteen.likeservice.mapper.LikeMapper;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.LikeService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class LikeServiceImpl implements LikeService {

    private final PostClient postClient;
    private final LikeMapper likeMapper;
    private final LikeRepository likeRepository;

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

    private void checkPostExistsOrThrowException(Long postId) {
        try {
            postClient.checkPostExists(postId);
        } catch (FeignException.NotFound e) {
            log.warn("Exception during interaction with post-client: post with id '{}' not found", postId);
            throw new NotFoundException(String.format("Post with id '%d' not found", postId));
        }
    }
}
