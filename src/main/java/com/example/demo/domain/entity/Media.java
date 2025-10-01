package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Media 엔티티
 *
 * <p><b>저장 정보:</b></p>
 * <ul>
 *   <li>S3 원본 이미지 경로 (storagePath) - EXIF 포함, 관리자/분석용</li>
 *   <li>이미지 크기 (width, height, bytes)</li>
 *   <li>EXIF 메타데이터 및 공개 URL (extra_info JSONB 컬럼에 저장)</li>
 * </ul>
 *
 * <p><b>extra_info JSONB 구조:</b></p>
 * <pre>
 * {
 *   "sanitizedUrl": "https://...sanitized/random123_file.jpg",  // 공개 이미지 URL (EXIF 제거)
 *   "cameraMake": "Apple",
 *   "cameraModel": "iPhone 14 Pro",
 *   "capturedAt": "2025-01-15T14:30:00",
 *   "gpsLatitude": 37.5665,
 *   "gpsLongitude": 126.9780
 * }
 * </pre>
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

    /**
     * S3 원본 이미지 경로 (EXIF 메타데이터 포함)
     * <p>관리자 및 분석용으로만 사용</p>
     * <p>공개 이미지 URL은 extra_info.sanitizedUrl에 저장</p>
     */
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
     * EXIF 메타데이터 및 기타 정보 (JSONB)
     * <p>구조: { sanitizedUrl, cameraMake, cameraModel, capturedAt, gpsLatitude, gpsLongitude }</p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_info", columnDefinition = "jsonb")
    private Map<String, Object> extraInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
