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
@Table(name = "subscriptions",
        schema = "feed_service",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"})
)
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
