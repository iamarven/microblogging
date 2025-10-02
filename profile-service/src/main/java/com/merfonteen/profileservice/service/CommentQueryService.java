package com.merfonteen.profileservice.service;

import com.merfonteen.profileservice.dto.CommentItemDto;
import com.merfonteen.profileservice.dto.CommentPageDto;
import com.merfonteen.profileservice.dto.CommentsSearchRequest;
import com.merfonteen.profileservice.mapper.CommentMapper;
import com.merfonteen.profileservice.model.CommentReadModel;
import com.merfonteen.profileservice.model.cursors.CommentCursor;
import com.merfonteen.profileservice.repository.CommentReadModelRepository;
import com.merfonteen.profileservice.util.CursorCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.merfonteen.profileservice.config.RedisConfig.POST_COMMENTS_CACHE;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentQueryService {
    private final CursorCodec cursorCodec;
    private final CommentMapper commentMapper;
    private final CommentReadModelRepository commentReadModelRepository;

    @Cacheable(value = POST_COMMENTS_CACHE, key = "#postId + ':' + #request.limit + ':' + #request.cursor")
    public CommentPageDto getComments(Long postId, CommentsSearchRequest request) {
        log.debug("Getting comments on post='{}', limit='{}'", postId, request.getLimit());
        Pageable page = Pageable.ofSize(Math.min(Math.max(request.getLimit(), 1), 100));
        List<CommentReadModel> comments;

        Optional<CommentCursor> currentCursor = cursorCodec.decodeCommentCursor(request.getCursor());
        if (request.getCursor().isEmpty()) {
            comments = commentReadModelRepository.findLatestByPostId(postId, page);
        } else {
            comments = commentReadModelRepository.findByPostIdAfterCursor(
                    postId,
                    currentCursor.get().createdAt(),
                    currentCursor.get().id(),
                    page
            );
        }

        List<CommentItemDto> items = comments.stream()
                .map(commentMapper::toDto)
                .toList();

        String nextCursor = comments.size() == page.getPageSize() ?
                cursorCodec.encodeCommentCursor(comments.getLast().getCreatedAt(),
                                                comments.getLast().getCommentId())
                : null;

        return new CommentPageDto(items, nextCursor);
    }

    @Cacheable(value = POST_COMMENTS_CACHE, key = "#postIds + ':' + #topN")
    public List<CommentReadModel> findTopNByPostIds(long[] postIds, int topN) {
        return commentReadModelRepository.findTopNByPostIds(postIds, topN);
    }
}
