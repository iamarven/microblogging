package com.merfonteen.profileservice.repository;

import com.merfonteen.profileservice.model.CommentReadModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CommentReadModelRepository extends JpaRepository<CommentReadModel, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM profile_service.comment_read_model WHERE comment_id = :commentId", nativeQuery = true)
    int deleteSilently(@Param("commentId") Long commentId);

    @Query("""
            SELECT c FROM CommentReadModel c
            WHERE c.postId = :postId
            ORDER BY c.createdAt DESC, c.commentId DESC
            """
    )
    List<CommentReadModel> findLatestByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("""
            SELECT c FROM CommentReadModel c
            WHERE c.postId = :postId AND 
                        (c.createdAt < :createdAt OR (c.createdAt = :createdAt AND c.commentId < :commentId))
                                    ORDER BY c.createdAt DESC, c.commentId DESC
            """)
    List<CommentReadModel> findByPostIdAfterCursor(@Param("postId") Long postId,
                                                   @Param("createdAt") Instant createdAt,
                                                   @Param("commentId") Long commentId,
                                                   Pageable pageable);

    @Query(value = """
            SELECT * FROM (
                            SELECT c.*, ROW_NUMBER() OVER (PARTITION BY c.post_id ORDER BY c.created_at DESC, c.comment_id DESC)
                            as rank
                            FROM profile_service.comment_read_model c
                            WHERE c.post_id = ANY(:postIds)
                        ) t
                        WHERE t.rank <= :topN
            """, nativeQuery = true)
    List<CommentReadModel> findTopNByPostIds(@Param("postIds") long[] postIds,
                                             @Param("topN") int topN);
}
