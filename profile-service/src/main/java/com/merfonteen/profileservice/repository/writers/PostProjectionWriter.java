package com.merfonteen.profileservice.repository.writers;

import com.merfonteen.profileservice.model.PostReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface PostProjectionWriter extends JpaRepository<PostReadModel, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
               INSERT INTO profile_service.post_read_model(post_id, author_id, created_at, content, likes_count, comments_count)
                    VALUES (:postId, :authorId, :createdAt, :content, :likesCount, :commentsCount) 
                         ON CONFLICT (post_id) DO UPDATE 
                              SET author_id = EXCLUDED.author_id,
                                  created_at = EXCLUDED.created_at,
                                  content = EXCLUDED.content
            """, nativeQuery = true)
    int upsertPost(@Param("postId") Long postId,
                   @Param("authorId") Long authorId,
                   @Param("createdAt") Instant createdAt,
                   @Param("content") String content,
                   @Param("likesCount") long likesCount,
                   @Param("commentsCount") long commentsCount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE profile_service.post_read_model
                        SET comments_count = comments_count + :delta
                        WHERE post_id = :postId
            """, nativeQuery = true)
    void incrementPostComments(@Param("postId") Long postId, @Param("delta") Long delta);
}
