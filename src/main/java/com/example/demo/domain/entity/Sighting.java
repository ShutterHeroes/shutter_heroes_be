package com.example.demo.domain.entity;

import com.example.demo.domain.converter.DetectedByConverter;
import com.example.demo.domain.converter.VisibilityConverter;
import com.example.demo.domain.enums.DetectedBy;
import com.example.demo.domain.enums.Visibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sightings")
@Getter
@Setter
@NoArgsConstructor
public class Sighting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id")
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media media;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "detected_by", nullable = false)
    @ColumnTransformer(
        read = "detected_by::varchar",
        write = "?::app.detected_by"
    )
    @Convert(converter = DetectedByConverter.class)
    private DetectedBy detectedBy = DetectedBy.USER;

    @Column(name = "ai_confidence")
    private BigDecimal aiConfidence;

    @Column(nullable = false)
    @ColumnTransformer(
        read = "visibility::varchar",
        write = "?::app.visibility"
    )
    @Convert(converter = VisibilityConverter.class)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "address_text", columnDefinition = "TEXT")
    private String addressText;

    @Column(name = "geom", columnDefinition = "geometry(Point,4326)")
    private Point geom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
