package com.merfonteen.postservice.repository;

import com.merfonteen.postservice.model.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    Page<OutboxEvent> findAllBySentFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.sent = true WHERE e.id IN :ids AND e.sent = false")
    void markAsSent(@Param("ids") List<Long> ids);
}
