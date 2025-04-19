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

    @Column(nullable = true)
    private String fullName;

    @Column(nullable = true)
    private String bio;

    @Column(nullable = true)
    private String profileImageUrl;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean isActive;
}
