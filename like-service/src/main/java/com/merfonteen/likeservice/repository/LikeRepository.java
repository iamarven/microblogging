package com.merfonteen.likeservice.repository;

import com.merfonteen.likeservice.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Page<Like> findAllByPostId(Long postId, Pageable pageable);
    Optional<Like> findByPostIdAndUserId(Long postId, Long currentUserId);
    long countByPostId(Long postId);
}
