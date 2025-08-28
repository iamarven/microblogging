package com.merfonteen.feedservice.repository;

import com.merfonteen.feedservice.model.Feed;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {
    Page<Feed> findAllByUserId(Long userId, Pageable pageable);

    int deleteAllByPostId(Long postId);

    @Modifying
    @Query(value = """
            WITH victim AS (
                        SELECT id FROM feeds WHERE created_at < :date ORDER BY created_at LIMIT :batchSize 
                        )
                        DELETE FROM feeds f USING victim v WHERE f.id = v.id
            """, nativeQuery = true)
    int deleteFeedsBelowDate(@Param("date") Instant date, @Param("batchSize") int batchSize);
}
