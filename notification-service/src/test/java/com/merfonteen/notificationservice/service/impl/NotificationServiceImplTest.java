package com.merfonteen.notificationservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.notificationservice.client.FeedClient;
import com.merfonteen.notificationservice.client.PostClient;
import com.merfonteen.notificationservice.dto.NotificationResponse;
import com.merfonteen.notificationservice.dto.NotificationsPageResponse;
import com.merfonteen.notificationservice.dto.NotificationsSearchRequest;
import com.merfonteen.notificationservice.mapper.NotificationMapper;
import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.repository.NotificationRepository;
import com.merfonteen.notificationservice.service.redis.RedisCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.merfonteen.notificationservice.service.impl.NotificationServiceImplTest.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private PostClient postClient;

    @Mock
    private FeedClient feedClient;

    @Mock
    private RedisCounter redisCounter;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void testGetMyNotifications_ShouldReturnAllNotifications_WhenNoFilterProvided() {
        Notification notification = buildNotification();
        NotificationResponse notificationResponse = buildNotificationResponse(notification);
        List<NotificationResponse> notificationDtos = List.of(notificationResponse);
        PageRequest pageRequest = buildPageRequest();

        Page<Notification> pageMock = new PageImpl<>(List.of(notification));

        when(notificationMapper.buildPageRequest(PAGE, SIZE)).thenReturn(pageRequest);
        when(notificationRepository.findAllByReceiverId(USER_ID, pageRequest)).thenReturn(pageMock);
        when(notificationMapper.toDtos(pageMock.getContent())).thenReturn(notificationDtos);
        when(notificationMapper.buildNotificationsPageResponse(notificationDtos, pageMock))
                .thenReturn(buildNotificationPageResponse(notificationDtos));

        NotificationsPageResponse result = notificationService.getMyNotifications(USER_ID, buildSearchRequest(""));

        assertThat(result.getNotifications()).isEqualTo(notificationDtos);
    }

    @Test
    void testGetMyNotifications_ShouldReturnUnread_WhenFilterIsUnread() {
        Notification notification = buildNotification();
        NotificationResponse notificationResponse = buildNotificationResponse(notification);
        List<NotificationResponse> notificationDtos = List.of(notificationResponse);
        Page<Notification> pageMock = new PageImpl<>(List.of(notification));
        PageRequest pageRequest = buildPageRequest();

        when(notificationMapper.buildPageRequest(PAGE, SIZE)).thenReturn(pageRequest);
        when(notificationMapper.toDtos(pageMock.getContent())).thenReturn(notificationDtos);
        when(notificationRepository.findAllByReceiverIdAndIsReadFalse(USER_ID, pageRequest)).thenReturn(pageMock);
        when(notificationMapper.buildNotificationsPageResponse(notificationDtos, pageMock))
                .thenReturn(buildNotificationPageResponse(notificationDtos));

        NotificationsPageResponse result = notificationService.getMyNotifications(USER_ID, buildSearchRequest("unread"));

        assertThat(result.getNotifications()).isEqualTo(notificationDtos);
    }

    @Test
    void testCountUnreadNotifications_ShouldReturnCachedValue_WhenCacheHit() {
        when(redisCounter.getCachedValue(USER_ID)).thenReturn("100");

        Long result = notificationService.countUnreadNotifications(USER_ID);

        assertThat(result).isEqualTo(100L);
        verify(notificationRepository, never()).countAllByReceiverIdAndIsReadFalse(anyLong());
    }

    @Test
    void testCountUnreadNotifications_ShouldQueryDbAndCache_WhenCacheMiss() {
        when(redisCounter.getCachedValue(USER_ID)).thenReturn(null);
        when(notificationRepository.countAllByReceiverIdAndIsReadFalse(USER_ID)).thenReturn(100L);

        Long result = notificationService.countUnreadNotifications(USER_ID);

        assertThat(result).isEqualTo(100L);
        verify(notificationRepository, times(1)).countAllByReceiverIdAndIsReadFalse(USER_ID);
        verify(redisCounter).setCounter(USER_ID, result);
    }

    @Test
    void testMarkAsRead_ShouldUpdateNotificationAndDecrementCache_WhenFound() {
        Notification notification = buildNotification();
        Notification savedNotification = buildNotification(true);
        NotificationResponse notificationResponse = buildNotificationResponse(savedNotification);

        when(notificationRepository.findByIdAndReceiverId(NOTIFICATION_ID, USER_ID))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(savedNotification);
        when(notificationMapper.toDto(savedNotification)).thenReturn(notificationResponse);

        NotificationResponse result = notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

        assertThat(result.getIsRead()).isEqualTo(true);
        verify(redisCounter).decrementCounter(USER_ID);
    }

    @Test
    void testMarkAsRead_ShouldThrowException_WhenNotificationNotFound() {
        when(notificationRepository.findByIdAndReceiverId(NOTIFICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                notificationService.markAsRead(NOTIFICATION_ID, USER_ID));

        verify(notificationRepository, never()).save(any());
        verify(redisCounter, never()).decrementCounter(USER_ID);
    }

    @Test
    void testDeleteNotification_ShouldDeleteNotification_WhenExistsAndOwnedByUser() {
        Notification notification = buildNotification();
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        notificationService.deleteNotification(NOTIFICATION_ID, USER_ID);
        verify(notificationRepository).delete(notification);
    }

    @Test
    void testDeleteNotification_ShouldThrowBadRequestException_WhenNotificationNotOwnedByUser() {
        Notification notification = buildNotification(ANOTHER_USER_ID);

        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        assertThrows(BadRequestException.class, () ->
                notificationService.deleteNotification(NOTIFICATION_ID, USER_ID));

        verify(notificationRepository, never()).delete(any());
    }

    static class TestResources {
        static final Long NOTIFICATION_ID = 1L;
        static final Long USER_ID = 100L;
        static final Long ANOTHER_USER_ID = 200L;
        static final int PAGE = 0;
        static final int SIZE = 10;

        static PageRequest buildPageRequest() {
            return PageRequest.of(PAGE, SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        static NotificationsPageResponse buildNotificationPageResponse(List<NotificationResponse> notificationDtos) {
            return NotificationsPageResponse.builder()
                    .notifications(notificationDtos)
                    .build();
        }

        static Notification buildNotification() {
            return Notification.builder()
                    .id(NOTIFICATION_ID)
                    .receiverId(USER_ID)
                    .isRead(false)
                    .build();
        }

        static Notification buildNotification(Long receiverId) {
            return Notification.builder()
                    .id(NOTIFICATION_ID)
                    .receiverId(receiverId)
                    .isRead(false)
                    .build();
        }

        static Notification buildNotification(boolean isRead) {
            return Notification.builder()
                    .id(NOTIFICATION_ID)
                    .receiverId(USER_ID)
                    .isRead(isRead)
                    .build();
        }

        static NotificationResponse buildNotificationResponse(Notification notification) {
            return NotificationResponse.builder()
                    .id(notification.getId())
                    .receiverId(notification.getReceiverId())
                    .isRead(notification.getIsRead())
                    .build();
        }

        static NotificationsSearchRequest buildSearchRequest(String filter) {
            return NotificationsSearchRequest.builder()
                    .page(PAGE)
                    .size(SIZE)
                    .filterRaw(filter)
                    .build();
        }
    }
}