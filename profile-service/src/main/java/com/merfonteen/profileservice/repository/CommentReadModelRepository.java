package com.merfonteen.profileservice.repository;

import com.merfonteen.profileservice.model.CommentReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReadModelRepository extends JpaRepository<CommentReadModel, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM profile_service.post_read_model WHERE comment_id = :commentId", nativeQuery = true)
    int deleteSilently(@Param("commentId") Long commentId);
}
