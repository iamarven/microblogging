package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.entity.Post;
import com.merfonteen.postservice.exception.NotFoundException;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostRepository postRepository;

    @Override
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Post with id '%d' not found", id)));
        return postMapper.toDto(post);
    }
}
