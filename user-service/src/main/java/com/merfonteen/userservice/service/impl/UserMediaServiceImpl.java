package com.merfonteen.userservice.service.impl;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.userservice.client.MediaClient;
import com.merfonteen.userservice.model.User;
import com.merfonteen.userservice.repository.UserRepository;
import com.merfonteen.userservice.service.UserMediaService;
import com.merfonteen.userservice.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class UserMediaServiceImpl implements UserMediaService {
    private final MediaClient mediaClient;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public FileUploadResponse uploadProfileImage(MultipartFile file, Long currentUserId) {
        User user = findUserByIdOrThrowException(currentUserId);

        FileUploadResponse fileUploadResponse = mediaClient.uploadProfileImage(file, currentUserId);

        user.setProfileImageUrl(fileUploadResponse.getFileUrl());
        User saved = userRepository.save(user);
        log.info("User profile image was successfully uploaded: '{}'", saved);

        return fileUploadResponse;
    }

    @Transactional
    @Override
    public void deleteUserMediaFile(Long userId, String fileType, String fileName, Long currentUserId) {
        User user = findUserByIdOrThrowException(userId);
        AuthUtil.requireSelfAccess(userId, currentUserId);

        mediaClient.deleteUserMediaFile(fileType, fileName);

        user.setProfileImageUrl(null);
        userRepository.save(user);
        log.info("User profile image was successfully removed, user id: '{}'", userId);
    }

    private User findUserByIdOrThrowException(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found by id: " + userId));
    }
}
