package com.merfonteen.mediaservice.service;

import com.merfonteen.exceptions.FileStorageException;
import com.merfonteen.exceptions.InvalidFileException;
import com.merfonteen.mediaservice.config.MinioProperties;
import com.merfonteen.mediaservice.enums.FileType;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    @InjectMocks
    private FileStorageService service;

    @Test
    void uploadPostMedia_ShouldUploadAndReturnFileName() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());

        when(minioProperties.getMaxFileSize()).thenReturn(9_000_000L);
        when(minioProperties.getPostsBucket()).thenReturn("posts-media");
        when(minioProperties.getAllowedImageTypes()).thenReturn(List.of("image/jpeg", "image/jpg", "image/png"));

        String result = service.uploadPostMedia(file, 10L, 5L);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();

        assertEquals(minioProperties.getPostsBucket(), args.bucket());
        assertEquals(result, args.object());
        assertTrue(result.startsWith("post/5/10"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void uploadPostMedia_WhenFileEmpty_ShouldThrowException() {
        MultipartFile file = new MockMultipartFile("file", new byte[0]);
        assertThrows(InvalidFileException.class, () -> service.uploadPostMedia(file, 1L, 1L));
        verifyNoInteractions(minioClient);
    }

    @Test
    void uploadPostMedia_WhenInvalidType_ShouldThrowException() {
        MultipartFile file = new MockMultipartFile("file", "video.txt", "text/plain", "data".getBytes());
        assertThrows(InvalidFileException.class, () -> service.uploadPostMedia(file, 1L, 1L));
        verifyNoInteractions(minioClient);
    }

    @Test
    void uploadProfilePhoto_ShouldUploadAndReturnFileName() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "data".getBytes());

        when(minioProperties.getMaxFileSize()).thenReturn(5_000_000L);
        when(minioProperties.getProfilesBucket()).thenReturn("profile-photo");
        when(minioProperties.getAllowedImageTypes()).thenReturn(List.of("image/jpeg", "image/jpg", "image/png"));

        String result = service.uploadProfilePhoto(file, 3L);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();

        assertEquals(minioProperties.getProfilesBucket(), args.bucket());
        assertEquals(result, args.object());
        assertTrue(result.startsWith("profile/3"));
        assertTrue(result.endsWith(".png"));
    }

    @Test
    void uploadProfilePhoto_WhenInvalidImageType_ShouldThrowException() {
        MultipartFile file = new MockMultipartFile("file", "avatar.gif", "image/gif", "data".getBytes());
        assertThrows(InvalidFileException.class, () -> service.uploadProfilePhoto(file, 1L));
        verifyNoInteractions(minioClient);
    }

    @Test
    void deleteFile_ShouldRemoveObject() throws Exception {
        when(minioProperties.getPostsBucket()).thenReturn("posts-media");

        service.deleteFile("file.jpg", FileType.POST_MEDIA);

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertEquals(minioProperties.getPostsBucket(), captor.getValue().bucket());
        assertEquals("file.jpg", captor.getValue().object());
    }

    @Test
    void getFileUrl_ShouldReturnUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://url");
        when(minioProperties.getPostsBucket()).thenReturn("posts-media");

        String url = service.getFileUrl("file.jpg", FileType.POST_MEDIA);

        assertEquals("http://url", url);
        ArgumentCaptor<GetPresignedObjectUrlArgs> captor = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(captor.capture());

        assertEquals(minioProperties.getPostsBucket(), captor.getValue().bucket());
        assertEquals("file.jpg", captor.getValue().object());
        assertEquals(Method.GET, captor.getValue().method());
        assertEquals(TimeUnit.DAYS.toSeconds(7), captor.getValue().expiry());
    }

    @Test
    void getFileUrl_WhenGenerationFails_ShouldThrowException() throws Exception {
        when(minioProperties.getPostsBucket()).thenReturn("posts-media");
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenThrow(new RuntimeException("err"));
        assertThrows(FileStorageException.class, () -> service.getFileUrl("file.jpg", FileType.POST_MEDIA));
    }

}