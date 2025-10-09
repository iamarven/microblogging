package com.merfonteen.userservice.controller;

import com.merfonteen.userservice.api.UsersApi;
import com.merfonteen.userservice.model.FileUploadResponse;
import com.merfonteen.userservice.model.PrivateUserDto;
import com.merfonteen.userservice.model.PublicUserDto;
import com.merfonteen.userservice.model.UserCreateDto;
import com.merfonteen.userservice.model.UserUpdateDto;
import com.merfonteen.userservice.service.UserMediaService;
import com.merfonteen.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController implements UsersApi {
    private final UserService userService;
    private final UserMediaService userMediaService;

    public UserController(UserService userService, UserMediaService userMediaService) {
        this.userService = userService;
        this.userMediaService = userMediaService;
    }

    @Override
    public ResponseEntity<PrivateUserDto> getCurrentUser(Long xUserId) {
        return ResponseEntity.ok(userService.getCurrentUser(xUserId));
    }

    @Override
    public ResponseEntity<PublicUserDto> getUser(Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Override
    public ResponseEntity<PublicUserDto> createUser(UserCreateDto userCreateDto) {
        return ResponseEntity.ok(userService.createUser(userCreateDto));
    }

    @Override
    public ResponseEntity<PublicUserDto> updateUser(Long id, Long xUserId, UserUpdateDto userUpdateDto) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdateDto, xUserId));
    }

    @Override
    public ResponseEntity<PublicUserDto> deleteUser(Long id, Long xUserId) {
        return ResponseEntity.ok(userService.deleteUser(id, xUserId));
    }

    @Override
    public ResponseEntity<FileUploadResponse> uploadProfileImage(Long xUserId, MultipartFile file) {
        return ResponseEntity.ok(userMediaService.uploadProfileImage(file, xUserId));
    }

    @Override
    public ResponseEntity<Void> deleteMedia(Long id, String fileType, String fileName, Long xUserId) {
        userMediaService.deleteUserMediaFile(id, fileType, fileName, xUserId);
        return ResponseEntity.noContent().build();
    }
}
