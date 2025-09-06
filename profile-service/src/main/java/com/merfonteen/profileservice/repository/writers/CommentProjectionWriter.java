package com.merfonteen.profileservice.repository.writers;

import com.merfonteen.profileservice.model.CommentReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface CommentProjectionWriter extends JpaRepository<CommentReadModel, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                    INSERT INTO profile_service.comment_read_model(comment_id, post_id, author_id, content, created_at, likes_count)
                    VALUES (:commentId, :postId, :authorId, :content, :createdAt, :likesCount)
                                ON CONFLICT (comment_id) DO UPDATE
                                    SET post_id = EXCLUDED.post_id,
                                        author_id = EXCLUDED.author_id,
                                        content = EXCLUDED.content,
                                        created_at = EXCLUDED.created_at
            """, nativeQuery = true)
    int upsertComment(@Param("commentId") Long commentId,
                      @Param("postId") Long postId,
                      @Param("authorId") Long authorId,
                      @Param("content") String content,
                      @Param("createdAt") Instant createdAt,
                      @Param("likesCount") long likesCount);

}
