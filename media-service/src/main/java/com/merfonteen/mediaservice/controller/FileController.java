package com.merfonteen.mediaservice.controller;

import com.merfonteen.mediaservice.dto.FileUploadResponse;
import com.merfonteen.mediaservice.enums.FileType;
import com.merfonteen.mediaservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RequiredArgsConstructor
@RequestMapping("/api/files")
@RestController
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/posts/{postId}/media")
    public ResponseEntity<FileUploadResponse> uploadPostMedia(@PathVariable Long postId,
                                                              @RequestParam("file")MultipartFile file,
                                                              @RequestHeader("X-User-Id") Long currentUserId) {

        String fileName = fileStorageService.uploadPostMedia(file, postId, currentUserId);
        String fileUrl = fileStorageService.getFileUrl(fileName, FileType.POST_MEDIA);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileName(fileName)
                .originalFileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadedAt(Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }
}
