package com.example.demo.domain.entity;

import com.example.demo.domain.converter.DetectedByConverter;
import com.example.demo.domain.converter.VisibilityConverter;
import com.example.demo.domain.enums.DetectedBy;
import com.example.demo.domain.enums.Visibility;
import com.example.demo.domain.web.dto.SightingUpdateRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Point;
import org.springframework.util.StringUtils;

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

    /**
     * Sighting 정보 업데이트
     *
     * @param request 업데이트 요청 DTO
     */
    public void update(SightingUpdateRequest request) {
        // title 업데이트
        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }

        // description 업데이트
        if (request.getDescription() != null) {
            this.description = request.getDescription();
        }

        // visibility 업데이트
        if (StringUtils.hasText(request.getVisibility())) {
            this.visibility = parseVisibility(request.getVisibility());
        }

        // occurredAt 업데이트
        if (request.getOccurredAt() != null) {
            this.occurredAt = request.getOccurredAt();
        }

        // addressText 업데이트
        if (request.getAddressText() != null) {
            this.addressText = request.getAddressText();
        }

        // updatedAt은 @PreUpdate에서 자동 설정됨
    }

    /**
     * 문자열을 Visibility enum으로 변환
     */
    private Visibility parseVisibility(String visibility) {
        try {
            return Visibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid visibility value: " + visibility + ". Must be PUBLIC or PRIVATE");
        }
    }
}
