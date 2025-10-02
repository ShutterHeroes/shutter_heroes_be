package com.example.demo.domain.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sighting 목록 조회용 네이티브 쿼리 결과 Projection
 *
 * <p>페이징 처리된 목록 조회에 사용되며, 상세 정보보다 간략한 정보만 포함합니다.</p>
 */
public interface SightingListRow {
    UUID getId();
    String getTitle();
    String getDescription();
    LocalDateTime getOccurredAt();
    String getDetectedBy();
    BigDecimal getAiConfidence();
    String getVisibility();
    Boolean getIsVerified();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    // User 정보
    String getDisplayName();

    // Species 정보
    String getCommonNameKo();
    String getCommonNameEn();
    String getScientificName();
    String getStatus();

    // Media 정보
    String getSanitizedUrl();  // 공개 이미지 URL (EXIF 제거됨)

    // GPS 정보 (GeoJSON)
    String getGeom();
}
