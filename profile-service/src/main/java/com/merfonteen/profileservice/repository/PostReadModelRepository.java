package com.merfonteen.profileservice.repository;

import com.merfonteen.profileservice.model.PostReadModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostReadModelRepository extends JpaRepository<PostReadModel, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostReadModel p WHERE p.postId = :postId")
    int deleteSilently(@Param("postId") Long postId);

    @Query("""
            SELECT p FROM PostReadModel p 
            WHERE p.authorId = :authorId
            ORDER BY p.createdAt DESC, p.postId
            """)
    List<PostReadModel> findLatestByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    @Query("""
            SELECT p FROM PostReadModel p 
            WHERE p.authorId = :authorId AND 
                        (p.createdAt < :createdAt OR (p.createdAt = :createdAt AND p.postId < :postId))
            ORDER BY p.createdAt DESC, p.postId
            """)
    List<PostReadModel> findByAuthorIdAfterCursor(@Param("authorId") Long authorId,
                                                  @Param("createdAt") Instant createdAt,
                                                  @Param("postId") Long postId,
                                                  Pageable pageable);
}
