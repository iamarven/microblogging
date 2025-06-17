package com.merfonteen.mediaservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "minio")
@Data
@Component
public class MinioProperties {
    private String url;
    private String accessKey;
    private String secretKey;
    private String postsBucket = "posts-media";
    private String profilesBucket = "profile-photos";
    private Long maxFileSize = 10485760L;

    private List<String> allowedImageTypes = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );

    private List<String> allowedVideoTypes = Arrays.asList(
            "video/mp4", "video/avi", "video/mov", "video/wmv"
    );
}
