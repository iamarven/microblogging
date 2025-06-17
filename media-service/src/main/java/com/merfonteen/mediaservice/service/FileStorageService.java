package com.merfonteen.mediaservice.service;

import com.merfonteen.exceptions.FileStorageException;
import com.merfonteen.exceptions.InvalidFileException;
import com.merfonteen.mediaservice.config.MinioProperties;
import com.merfonteen.mediaservice.enums.FileType;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @PostConstruct
    public void init() {
        createBucketIfNotExists(minioProperties.getPostsBucket());
        createBucketIfNotExists(minioProperties.getProfilesBucket());
    }

    public String uploadPostMedia(MultipartFile file, Long postId, Long currentUserId) {
        validate(file, FileType.POST_MEDIA);

        String fileName = generateFileName(file, postId, currentUserId, "post");
        String bucket = minioProperties.getPostsBucket();

        return uploadFile(file, bucket, fileName);
    }

    public String uploadProfilePhoto(MultipartFile file, Long userId) {
        validateImageFile(file);

        String fileName = generateFileName(file, null, userId, "profile");
        String bucketName = minioProperties.getProfilesBucket();

        return uploadFile(file, bucketName, fileName);
    }

    public void deleteFile(String fileName, FileType fileType) {
        try {
            String bucketName = getBucketName(fileType);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("Deleted file: {}", fileName);
        } catch (Exception e) {
            log.error("Error deleting file: {}", fileName, e);
            throw new FileStorageException("Failed to delete file: " + fileName);
        }
    }

    public String getFileUrl(String fileName, FileType fileType) {
        try {
            String bucketName = getBucketName(fileType);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL for file: {}", fileName, e);
            throw new FileStorageException("Failed to generate URL for file: " + fileName);
        }
    }

    private void validateImageFile(MultipartFile file) {
        validateFile(file, FileType.PROFILE_PHOTO);

        String contentType = file.getContentType();
        if (!minioProperties.getAllowedImageTypes().contains(contentType)) {
            throw new InvalidFileException("Invalid image type: " + contentType);
        }
    }

    private void validateFile(MultipartFile file, FileType fileType) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > minioProperties.getMaxFileSize()) {
            throw new InvalidFileException("File size exceeds maximum allowed size");
        }

        String contentType = file.getContentType();
        if (fileType == FileType.POST_MEDIA) {
            if (!isValidMediaType(contentType)) {
                throw new InvalidFileException("Invalid file type: " + contentType);
            }
        }
    }

    private String getBucketName(FileType fileType) {
        return switch (fileType) {
            case POST_MEDIA -> minioProperties.getPostsBucket();
            case PROFILE_PHOTO -> minioProperties.getProfilesBucket();
        };
    }

    private String uploadFile(MultipartFile file, String bucket, String fileName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Successfully uploaded file: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.error("Error uploading file: {}", fileName, e);
            throw new FileStorageException("Failed to upload file: " + fileName);
        }
    }

    private void validate(MultipartFile file, FileType fileType) {
        if(file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if(file.getSize() > minioProperties.getMaxFileSize()) {
            throw new InvalidFileException("File size exceeded maximum allowed size");
        }

        String contentType = file.getContentType();
        if(fileType == FileType.POST_MEDIA) {
            if(!isValidMediaType(contentType)) {
                throw new InvalidFileException("Invalid image type: " + contentType);
            }
        }
    }

    private String generateFileName(MultipartFile file, Long postId, Long userId, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        if (postId != null) {
            return String.format("%s/%d/%s_%s_%s%s", prefix, userId, postId, timestamp, uuid, extension);
        } else {
            return String.format("%s/%d/%s_%s%s", prefix, userId, timestamp, uuid, extension);
        }
    }

    private boolean isValidMediaType(String contentType) {
        return minioProperties.getAllowedImageTypes().contains(contentType) ||
                minioProperties.getAllowedVideoTypes().contains(contentType);
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error creating bucket: {}", bucketName, e);
            throw new FileStorageException("Failed to create bucket: " + bucketName);
        }
    }
}
