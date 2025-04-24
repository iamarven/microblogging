package com.merfonteen.userservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users", schema = "user_service")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "full_name", nullable = true)
    private String fullName;

    @Column(nullable = true)
    private String bio;

    @Column(name = "profile_image_url", nullable = true)
    private String profileImageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
