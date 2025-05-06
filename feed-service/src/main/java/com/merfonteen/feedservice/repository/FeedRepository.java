package com.merfonteen.feedservice.repository;

import com.merfonteen.feedservice.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {
    List<Feed> findAllByUserId(Long userId);
}
