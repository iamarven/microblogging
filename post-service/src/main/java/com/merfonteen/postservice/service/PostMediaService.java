package com.merfonteen.postservice.service;

import com.merfonteen.dtos.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostMediaService {
    List<String> getMediaUrlsForPost(Long postId);
    FileUploadResponse uploadMediaToPost(Long postId, MultipartFile file, Long currentUserId);
    void deletePostMedia(Long postId, String fileType, String fileName, Long currentUserId);
}
