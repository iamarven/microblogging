package com.merfonteen.notificationservice.mapper;

import com.merfonteen.notificationservice.dto.NotificationResponse;
import com.merfonteen.notificationservice.dto.NotificationsPageResponse;
import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.model.enums.NotificationType;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toDto(Notification notification);

    List<NotificationResponse> toDtos(List<Notification> notifications);

    default PageRequest buildPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    default NotificationsPageResponse buildNotificationsPageResponse(List<NotificationResponse> notificationDtos,
                                                                     Page<Notification> userNotifications) {
        return NotificationsPageResponse.builder()
                .notifications(notificationDtos)
                .currentPage(userNotifications.getNumber())
                .totalPages(userNotifications.getTotalPages())
                .totalElements(userNotifications.getTotalElements())
                .isLastPage(userNotifications.isLast())
                .build();
    }

    default Notification buildNotification(Long senderId, Long receiverId, Long entityId, NotificationType type) {
        String message = "";
        switch (type) {
            case LIKE -> message =
                    String.format("User with id '%d' has liked your post with id '%d'", senderId, entityId);
            case POST -> message =
                    String.format("User with id '%d' has published a new post with id '%d'", senderId, entityId);
            case SUBSCRIPTION -> message =
                    String.format("User with id '%d' has just subscribed to user with id '%d'", senderId, receiverId);
            case COMMENT -> message =
                    String.format("User with id '%d' has just left comment '%d' for you",
                            senderId, entityId);
        }
        return Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .entityId(entityId)
                .type(type)
                .message(message)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
    }
}
