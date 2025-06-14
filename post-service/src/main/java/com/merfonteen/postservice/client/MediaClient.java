package com.merfonteen.postservice.client;

import com.merfonteen.dtos.FileUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service", url = "${media-service.url}")
public interface MediaClient {

    @PostMapping("/api/files/{postId}/media")
    FileUploadResponse uploadPostMedia(@PathVariable Long postId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestHeader("X-User-Id") Long currentUserId);
}
