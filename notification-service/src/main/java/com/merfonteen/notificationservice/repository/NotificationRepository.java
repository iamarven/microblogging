package com.merfonteen.notificationservice.repository;

import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByReceiverId(Long receiverId, Pageable pageable);
    Page<Notification> findAllByReceiverIdAndIsReadTrue(Long receiverId, Pageable pageable);
    Page<Notification> findAllByReceiverIdAndIsReadFalse(Long receiverId, Pageable pageable);
    List<Notification> findAllByEntityIdAndType(Long entityId, NotificationType type);
    long countAllByReceiverIdAndIsReadFalse(Long receiverId);
    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);
    void deleteByEntityIdAndType(Long entityId, NotificationType type);
}
