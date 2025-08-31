package com.merfonteen.userservice.client;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.userservice.util.MediaClientFallBackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "media-service",
        url = "${media-service.url}",
        fallbackFactory = MediaClientFallBackFactory.class
)
public interface MediaClient {

    @PostMapping("/api/files/profile/photo")
    FileUploadResponse uploadProfileImage(@RequestParam("file") MultipartFile file,
                                          @RequestHeader("X-User-Id") Long userId);

    @DeleteMapping("/api/files/{fileType}/{fileName}")
    void deleteUserMediaFile(@PathVariable String fileType,
                             @PathVariable String fileName);
}
