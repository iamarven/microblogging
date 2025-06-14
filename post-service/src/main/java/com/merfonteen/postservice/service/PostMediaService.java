package com.merfonteen.postservice.service;

import com.merfonteen.dtos.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PostMediaService {
    FileUploadResponse uploadMediaToPost(Long postId, MultipartFile file, Long currentUserId);
}
