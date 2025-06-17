package com.merfonteen.userservice.controller;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.userservice.dto.PublicUserDto;
import com.merfonteen.userservice.dto.UserCreateDto;
import com.merfonteen.userservice.dto.PrivateUserDto;
import com.merfonteen.userservice.dto.UserUpdateDto;
import com.merfonteen.userservice.service.UserMediaService;
import com.merfonteen.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RequestMapping("/api/users")
@RestController
public class UserController {

    private final UserService userService;
    private final UserMediaService userMediaService;

    public UserController(UserService userService, UserMediaService userMediaService) {
        this.userService = userService;
        this.userMediaService = userMediaService;
    }

    @GetMapping("/me")
    public ResponseEntity<PrivateUserDto> getCurrentUser(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(userService.getCurrentUser(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicUserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<PublicUserDto> createUser(@RequestBody @Valid UserCreateDto userCreateDto) {
        PublicUserDto createdUser = userService.createUser(userCreateDto);
        URI location = URI.create("/api/users/" + createdUser.getId());
        return ResponseEntity.created(location).body(createdUser);
    }

    @PatchMapping("/upload/profile-image")
    public ResponseEntity<FileUploadResponse> uploadProfileImage(@RequestParam(name = "file") MultipartFile file,
                                                                 @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(userMediaService.uploadProfileImage(file, currentUserId));
    }

    @DeleteMapping("/{id}/delete/media/{fileType}/{fileName}")
    public ResponseEntity<Void> deleteUserMedia(@PathVariable("id") Long id,
                                                @PathVariable String fileType,
                                                @PathVariable String fileName,
                                                @RequestHeader("X-User-Id") Long currentUserId) {
        userMediaService.deleteUserMediaFile(id, fileType, fileName, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PublicUserDto> updateUserProfile(@PathVariable("id") Long id,
                                                             @RequestBody UserUpdateDto userUpdateDto,
                                                             @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(userService.updateUser(id, userUpdateDto, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PublicUserDto> deleteUser(@PathVariable("id") Long id,
                                              @RequestHeader("X-User-Id") Long currentUserId) {

        return ResponseEntity.ok(userService.deleteUser(id, currentUserId));
    }
}
