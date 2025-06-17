package com.merfonteen.postservice.util;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.exceptions.FileStorageException;
import com.merfonteen.postservice.client.MediaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class MediaClientFallBackFactory implements FallbackFactory<MediaClient> {

    @Override
    public MediaClient create(Throwable cause) {
        return new MediaClient() {
            @Override
            public FileUploadResponse uploadPostMedia(Long postId, MultipartFile file, Long currentUserId) {
                log.error("Fallback: Failed to upload media for postId={}, userId={}", postId, currentUserId, cause);
                throw new FileStorageException("Media service unavailable or returned error while uploading.");
            }

            @Override
            public void deletePostMedia(String fileType, String fileName) {
                log.error("Fallback: Failed to delete media fileType={}, fileName={}", fileType, fileName, cause);
                throw new FileStorageException("Media service unavailable or returned error while deleting.");
            }
        };
    }

}
