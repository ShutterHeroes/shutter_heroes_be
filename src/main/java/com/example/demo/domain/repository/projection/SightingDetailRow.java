package com.example.demo.domain.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sighting 상세 조회용 네이티브 쿼리 결과 Projection
 *
 * <p>목록 조회보다 더 상세한 정보를 포함합니다 (AI Detection, EXIF 정보 등)</p>
 */
public interface SightingDetailRow {
    // Sighting 기본 정보
    UUID getId();
    String getTitle();
    String getDescription();
    LocalDateTime getOccurredAt();
    String getDetectedBy();
    BigDecimal getAiConfidence();
    String getVisibility();
    Boolean getIsVerified();
    String getAddressText();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    // User 정보
    UUID getUserId();
    String getDisplayName();
    String getUserEmail();

    // Species 정보
    UUID getSpeciesId();
    String getCommonNameKo();
    String getCommonNameEn();
    String getScientificName();
    String getStatus();

    // Media 정보
    UUID getMediaId();
    String getSanitizedUrl();      // 공개 이미지 URL (EXIF 제거됨)
    String getStoragePath();        // 원본 이미지 URL (EXIF 포함, 소유자만)
    String getMimeType();
    Long getBytes();
    Integer getWidth();
    Integer getHeight();

    // EXIF 정보 (Media의 extra_info에서 추출)
    String getCameraMake();
    String getCameraModel();
    String getCapturedAt();
    Double getGpsLatitude();
    Double getGpsLongitude();

    // GPS 정보 (GeoJSON)
    String getGeom();
}
