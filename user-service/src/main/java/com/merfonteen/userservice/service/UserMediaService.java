package com.merfonteen.userservice.service;

import com.merfonteen.dtos.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserMediaService {
    FileUploadResponse uploadProfileImage(MultipartFile file, Long currentUserId);
    void deleteUserMediaFile(Long userId, String fileType, String fileName, Long currentUserId);
}
