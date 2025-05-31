package com.merfonteen.notificationservice.service.impl;

import com.merfonteen.notificationservice.client.FeedClient;
import com.merfonteen.notificationservice.client.PostClient;
import com.merfonteen.notificationservice.dto.NotificationDto;
import com.merfonteen.notificationservice.dto.NotificationsPageDto;
import com.merfonteen.notificationservice.exception.BadRequestException;
import com.merfonteen.notificationservice.exception.NotFoundException;
import com.merfonteen.notificationservice.mapper.NotificationMapper;
import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.repository.NotificationRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private PostClient postClient;

    @Mock
    private FeedClient feedClient;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void testGetMyNotifications_ShouldReturnAllNotifications_WhenNoFilterProvided() {
        Long userId = 123L;
        int page = 0;
        int size = 5;

        Notification notification = Notification.builder()
                .id(1L)
                .receiverId(userId)
                .isRead(false)
                .build();

        Page<Notification> pageMock = new PageImpl<>(List.of(notification));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(notificationRepository.findAllByReceiverId(eq(userId), any(PageRequest.class)))
                .thenReturn(pageMock);

        NotificationDto notificationDto = NotificationDto.builder()
                .id(1L)
                .receiverId(userId)
                .isRead(true)
                .build();

        when(notificationMapper.toDtos(pageMock.getContent()))
                .thenReturn(List.of(notificationDto));

        NotificationsPageDto result = notificationService.getMyNotifications(userId, page, size, null);

        assertNotNull(result);
        assertEquals(1, result.getNotifications().size());
        assertEquals(true, result.getNotifications().get(0).getIsRead());
        verify(notificationRepository).saveAll(pageMock);
        verify(redisTemplate.opsForValue()).set("user:notifications:count:" + userId, "0");
    }

    @Test
    void testGetMyNotifications_ShouldReturnUnread_WhenFilterIsUnread() {
        Long userId = 123L;
        int page = 0;
        int size = 5;

        Notification notification = Notification.builder()
                .id(2L)
                .receiverId(userId)
                .isRead(false)
                .build();

        Page<Notification> pageMock = new PageImpl<>(List.of(notification));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(notificationRepository.findAllByReceiverIdAndIsReadFalse(eq(userId), any(PageRequest.class)))
                .thenReturn(pageMock);
        when(notificationMapper.toDtos(pageMock.getContent())).thenReturn(List.of(new NotificationDto()));

        NotificationsPageDto result = notificationService.getMyNotifications(userId, page, size, "unread");

        assertNotNull(result);
        assertEquals(1, result.getNotifications().size());
        verify(notificationRepository).saveAll(pageMock);
        verify(redisTemplate.opsForValue()).set("user:notifications:count:" + userId, "0");
    }

    @Test
    void getMyNotifications_ShouldCapSizeTo100_WhenSizeIsGreaterThan100() {
        Long userId = 1L;
        int tooLargeSize = 200;

        Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList());

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(notificationRepository.findAllByReceiverId(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        when(notificationMapper.toDtos(anyList())).thenReturn(Collections.emptyList());

        NotificationsPageDto result = notificationService.getMyNotifications(userId, 0, tooLargeSize, null);

        assertEquals(0, result.getNotifications().size());
        verify(notificationRepository).findAllByReceiverId(eq(userId), argThat(p -> p.getPageSize() == 100));
    }


    @Test
    void testCountUnreadNotifications_ShouldReturnCachedValue_WhenCacheHit() {
        Long userId = 1L;
        String cacheKey = "user:notifications:unread:count:" + userId;
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(cacheKey)).thenReturn("5");

        Long result = notificationService.countUnreadNotifications(userId);


        assertEquals(5L, result);
        verify(notificationRepository, never()).countAllByReceiverIdAndIsReadFalse(anyLong());
    }

    @Test
    void testCountUnreadNotifications_ShouldQueryDbAndCache_WhenCacheMiss() {
        Long userId = 1L;
        String cacheKey = "user:notifications:unread:count:" + userId;

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(cacheKey)).thenReturn(null);
        when(notificationRepository.countAllByReceiverIdAndIsReadFalse(userId)).thenReturn(3L);

        Long result = notificationService.countUnreadNotifications(userId);

        assertEquals(3L, result);
        verify(notificationRepository).countAllByReceiverIdAndIsReadFalse(userId);
        verify(valueOps).set(eq(cacheKey), eq("3"), any(Duration.class));
    }

    @Test
    void testMarkAsRead_ShouldUpdateNotificationAndDecrementCache_WhenFound() {
        Long notificationId = 1L;
        Long userId = 100L;

        Notification notification = Notification.builder()
                .id(notificationId)
                .receiverId(userId)
                .isRead(false)
                .build();

        Notification savedNotification = Notification.builder()
                .id(notificationId)
                .receiverId(userId)
                .isRead(true)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(notificationRepository.findByIdAndReceiverId(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(savedNotification);
        when(notificationMapper.toDto(savedNotification)).thenReturn(new NotificationDto());

        NotificationDto result = notificationService.markAsRead(notificationId, userId);

        assertNotNull(result);
        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
        verify(redisTemplate.opsForValue()).decrement("user:notifications:unread:count:" + userId);
    }

    @Test
    void testMarkAsRead_ShouldThrowException_WhenNotificationNotFound() {
        Long notificationId = 1L;
        Long userId = 100L;

        when(notificationRepository.findByIdAndReceiverId(notificationId, userId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                notificationService.markAsRead(notificationId, userId));

        assertTrue(exception.getMessage().contains("Doesn't exist notification"));
        verify(notificationRepository, never()).save(any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void testDeleteNotification_ShouldDeleteNotification_WhenExistsAndOwnedByUser() {
        Long notificationId = 1L;
        Long userId = 10L;

        Notification notification = Notification.builder()
                .id(notificationId)
                .receiverId(userId)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationMapper.toDto(notification)).thenReturn(new NotificationDto());

        NotificationDto result = notificationService.deleteNotification(notificationId, userId);

        assertNotNull(result);
        verify(notificationRepository).delete(notification);
        verify(notificationMapper).toDto(notification);
    }

    @Test
    void testDeleteNotification_ShouldThrowNotFoundException_WhenNotificationDoesNotExist() {
        Long notificationId = 1L;
        Long userId = 10L;

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                notificationService.deleteNotification(notificationId, userId));

        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void testDeleteNotification_ShouldThrowBadRequestException_WhenNotificationNotOwnedByUser() {
        Long notificationId = 1L;
        Long userId = 10L;
        Long anotherUserId = 20L;

        Notification notification = Notification.builder()
                .id(notificationId)
                .receiverId(anotherUserId)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(BadRequestException.class, () ->
                notificationService.deleteNotification(notificationId, userId));

        verify(notificationRepository, never()).delete(any());
    }

}