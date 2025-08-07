package com.merfonteen.feedservice.repository;

import com.merfonteen.feedservice.model.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    Page<OutboxEvent> findAllBySentFalse(Pageable pageable);
}
