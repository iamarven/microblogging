package com.merfonteen.feedservice.repository;

import com.merfonteen.feedservice.model.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {
    Page<Feed> findAllByUserId(Long userId, Pageable pageable);
    int deleteAllByPostId(Long postId);
}
