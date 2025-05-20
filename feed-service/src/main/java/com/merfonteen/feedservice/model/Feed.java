package com.merfonteen.feedservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "feeds",
        schema = "feed_service",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class Feed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feed feed = (Feed) o;
        return Objects.equals(id, feed.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
