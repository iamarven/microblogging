package com.merfonteen.notificationservice.mapper;

import com.merfonteen.notificationservice.dto.NotificationDto;
import com.merfonteen.notificationservice.model.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface NotificationMapper {
    NotificationDto toDto(Notification notification);
    Notification toEntity(NotificationDto dto);
    List<NotificationDto> toDtos(List<Notification> notifications);
}
