package com.merfonteen.mediaservice.controller;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.dtos.FileUrlResponse;
import com.merfonteen.mediaservice.enums.FileType;
import com.merfonteen.mediaservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@RequestMapping("/api/files")
@RestController
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/posts/{postId}/media")
    public ResponseEntity<FileUploadResponse> uploadPostMedia(@PathVariable Long postId,
                                                              @RequestParam("file") MultipartFile file,
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

    @PostMapping("/profile/photo")
    public ResponseEntity<FileUploadResponse> uploadProfilePhoto(@RequestParam("file") MultipartFile file,
                                                                 @RequestHeader("X-User-Id") Long userId) {

        String fileName = fileStorageService.uploadProfilePhoto(file, userId);
        String fileUrl = fileStorageService.getFileUrl(fileName, FileType.PROFILE_PHOTO);

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

    @DeleteMapping("/{fileType}/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileType,
            @PathVariable String fileName,
            @RequestHeader("X-User-Id") Long userId) {

        FileType type = FileType.valueOf(fileType.toUpperCase());
        fileStorageService.deleteFile(fileName, type);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileType}/{fileName}/url")
    public ResponseEntity<FileUrlResponse> getFileUrl(
            @PathVariable String fileType,
            @PathVariable String fileName) {

        FileType type = FileType.valueOf(fileType.toUpperCase());
        String fileUrl = fileStorageService.getFileUrl(fileName, type);

        FileUrlResponse response = FileUrlResponse.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        return ResponseEntity.ok(response);
    }
}
