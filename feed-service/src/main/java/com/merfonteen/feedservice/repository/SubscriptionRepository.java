package com.merfonteen.feedservice.repository;

import com.merfonteen.feedservice.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findSubscriptionByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    List<Subscription> findAllByFollowerId(Long followerId);
}
