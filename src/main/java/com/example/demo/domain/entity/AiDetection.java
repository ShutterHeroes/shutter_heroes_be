package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AI 동물 감지 결과 엔티티
 *
 * <p><b>저장 정보:</b></p>
 * <ul>
 *   <li>Media와 N:1 관계</li>
 *   <li>동물 명칭 (label)</li>
 *   <li>신뢰도 (score/confidence)</li>
 *   <li>Bounding box 좌표 (nullable, 나중에 추가 가능)</li>
 *   <li>추가 정보 (extra_info JSONB): scientificName, description 등</li>
 * </ul>
 */
@Entity
@Table(name = "ai_detections")
@Getter
@Setter
@NoArgsConstructor
public class AiDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    /**
     * 동물의 일반 명칭 (예: "Bengal Cat", "Golden Retriever")
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    /**
     * AI 신뢰도 (0.0 ~ 1.0)
     */
    @Column
    private BigDecimal score;

    /**
     * 추가 정보 (JSONB)
     * - scientificName: 학명 (예: "Felis catus")
     * - description: 설명
     * - 기타 Vision API 응답 정보
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_info", columnDefinition = "jsonb")
    private Map<String, Object> extraInfo;

    /**
     * Bounding box 좌표 (nullable, 향후 Object Detection 추가 시 사용)
     */
    @Column(name = "x_min")
    private BigDecimal xMin;

    @Column(name = "y_min")
    private BigDecimal yMin;

    @Column(name = "x_max")
    private BigDecimal xMax;

    @Column(name = "y_max")
    private BigDecimal yMax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
