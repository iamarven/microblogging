package com.merfonteen.notificationservice.dto;

import com.merfonteen.notificationservice.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotificationDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long entityId;
    private NotificationType type;
    private String message;
    private Instant createdAt;
}
