package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Media 엔티티
 *
 * <p><b>저장 정보:</b></p>
 * <ul>
 *   <li>S3 저장 경로 (storagePath)</li>
 *   <li>이미지 크기 (width, height, bytes)</li>
 *   <li>EXIF 메타데이터 (cameraMake, cameraModel, capturedAt)</li>
 * </ul>
 */
@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "mime_type", columnDefinition = "TEXT")
    private String mimeType;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column
    private Long bytes;

    @Column(columnDefinition = "TEXT")
    private String checksum;

    /**
     * EXIF: 카메라 제조사 (예: Apple, Samsung, Canon)
     */
    @Column(name = "camera_make", columnDefinition = "TEXT")
    private String cameraMake;

    /**
     * EXIF: 카메라 모델 (예: iPhone 14 Pro, Galaxy S23)
     */
    @Column(name = "camera_model", columnDefinition = "TEXT")
    private String cameraModel;

    /**
     * EXIF: 촬영 시간 (DateTimeOriginal)
     */
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
