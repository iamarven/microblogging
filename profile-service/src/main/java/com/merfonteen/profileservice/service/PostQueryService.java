package com.merfonteen.profileservice.service;

import com.merfonteen.profileservice.dto.PostItemDto;
import com.merfonteen.profileservice.dto.PostPageDto;
import com.merfonteen.profileservice.dto.PostsSearchRequest;
import com.merfonteen.profileservice.mapper.PostMapper;
import com.merfonteen.profileservice.model.PostReadModel;
import com.merfonteen.profileservice.model.cursors.PostCursor;
import com.merfonteen.profileservice.repository.PostReadModelRepository;
import com.merfonteen.profileservice.util.CursorCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostQueryService {
    private final PostMapper postMapper;
    private final CursorCodec cursorCodec;
    private final CommentQueryService commentQueryService;
    private final PostReadModelRepository postReadModelRepository;

    public PostPageDto getUserPosts(Long userId, PostsSearchRequest request) {
        log.debug("Getting user posts for id='{}', limit='{}'", userId, request.getLimit());
        Pageable page = Pageable.ofSize(Math.min(Math.max(request.getLimit(), 1), 100));
        List<PostReadModel> posts;

        Optional<PostCursor> currentCursor = cursorCodec.decodePostCursor(request.getCursor());
        if (currentCursor.isEmpty()) {
            posts = postReadModelRepository.findLatestByAuthorId(userId, page);
        } else {
            posts = postReadModelRepository.findByAuthorIdAfterCursor(
                    userId,
                    currentCursor.get().createdAt(),
                    currentCursor.get().id(),
                    page);
        }

        List<PostItemDto> items = posts.stream()
                .map(postMapper::toDto)
                .toList();

        if (request.isIncludeComments()) {
            items.forEach(item -> {
                var latestComments = commentQueryService.getLatestCommentsOnPostWithLimit(item.getPostId(), 10);
                item.getComments().addAll(latestComments);
            });
        }

        String nextCursor = posts.size() == page.getPageSize() ?
                cursorCodec.encodePostCursor(posts.getLast().getCreatedAt(),
                                             posts.getLast().getPostId())
                : null;

        return new PostPageDto(items, nextCursor);
    }
}
