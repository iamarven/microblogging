package com.merfonteen.commentservice.repository;

import com.merfonteen.commentservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByPostId(Long postId, Pageable pageable);
    Page<Comment> findAllByParentId(Long parentId, Pageable pageable);
    long countAllByPostId(Long postId);
    int deleteAllByPostId(Long postId);
}
