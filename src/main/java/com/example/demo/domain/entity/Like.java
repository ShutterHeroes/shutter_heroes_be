package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "likes", schema = "app")
@IdClass(Like.LikeId.class)
@Getter
@Setter
@NoArgsConstructor
public class Like {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sighting_id", nullable = false)
    private Sighting sighting;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Embeddable
    public static class LikeId implements Serializable {
        private UUID sighting;
        private UUID user;

        public LikeId() {}

        public LikeId(UUID sighting, UUID user) {
            this.sighting = sighting;
            this.user = user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LikeId likeId = (LikeId) o;
            return Objects.equals(sighting, likeId.sighting) && Objects.equals(user, likeId.user);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sighting, user);
        }
    }
}
