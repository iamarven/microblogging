package com.merfonteen.profileservice.repository;

import com.merfonteen.profileservice.model.PostReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReadModelRepository extends JpaRepository<PostReadModel,Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM profile_service.post_read_model WHERE post_id = :postId", nativeQuery = true)
    int deleteSilently(@Param("postId") Long postId);
}
