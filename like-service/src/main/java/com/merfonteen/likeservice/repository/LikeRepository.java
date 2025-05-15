package com.merfonteen.likeservice.repository;

import com.merfonteen.likeservice.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Page<Like> findAllByPostId(Long postId, Pageable pageable);
}
