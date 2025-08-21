package com.merfonteen.notificationservice.controller;

import com.merfonteen.notificationservice.dto.NotificationResponse;
import com.merfonteen.notificationservice.dto.NotificationsPageResponse;
import com.merfonteen.notificationservice.dto.NotificationsSearchRequest;
import com.merfonteen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationsPageResponse> getMyNotifications(@RequestHeader("X-User-Id") Long currentUserId,
                                                                        NotificationsSearchRequest searchRequest) {
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUserId, searchRequest));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(currentUserId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable("id") Long id,
                                                           @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") Long id,
                                                   @RequestHeader("X-User-Id") Long currentUserId) {
        notificationService.deleteNotification(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
