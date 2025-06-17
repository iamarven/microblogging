package com.merfonteen.postservice.client;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.postservice.util.MediaClientFallBackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "media-service",
        url = "${media-service.url}",
        fallbackFactory = MediaClientFallBackFactory.class
)
public interface MediaClient {

    @PostMapping("/api/files/{postId}/media")
    FileUploadResponse uploadPostMedia(@PathVariable Long postId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestHeader("X-User-Id") Long currentUserId);

    @DeleteMapping("/api/files/{fileType}/{fileName}")
    void deletePostMedia(@PathVariable String fileType,
                         @PathVariable String fileName);
}
