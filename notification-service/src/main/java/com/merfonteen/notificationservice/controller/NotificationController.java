package com.merfonteen.notificationservice.controller;

import com.merfonteen.notificationservice.dto.NotificationDto;
import com.merfonteen.notificationservice.dto.NotificationsPageDto;
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
    public ResponseEntity<NotificationsPageDto> getMyNotifications(@RequestHeader("X-User-Id") Long currentUserId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   @RequestParam(defaultValue = "ALL") String filter) {
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUserId, page, size, filter));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(currentUserId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable("id") Long id,
                                                      @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<NotificationDto> deleteNotification(@PathVariable("id") Long id,
                                                              @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(notificationService.deleteNotification(id, currentUserId));
    }
}
