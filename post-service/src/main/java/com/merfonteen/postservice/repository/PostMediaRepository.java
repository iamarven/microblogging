package com.merfonteen.postservice.repository;

import com.merfonteen.postservice.model.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findAllByPostId(Long postId);
    Optional<PostMedia> findByPostIdAndFileName(Long postId, String fileName);
}
