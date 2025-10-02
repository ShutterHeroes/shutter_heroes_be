package com.example.demo.domain.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sighting 상세 조회 응답 DTO
 *
 * <p>목록 조회보다 더 상세한 정보를 포함합니다.</p>
 */
@Getter
@AllArgsConstructor
public class SightingDetailResponse {
    // Sighting 기본 정보
    private final UUID id;
    private final String title;
    private final String description;
    private final LocalDateTime occurredAt;
    private final String detectedBy;
    private final BigDecimal aiConfidence;
    private final String visibility;
    private final Boolean isVerified;
    private final String addressText;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // User 정보
    private final UserInfo user;

    // Species 정보
    private final SpeciesInfo species;

    // Media 정보
    private final MediaInfo media;

    // GPS 정보 (GeoJSON)
    private final String geom;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private final UUID id;
        private final String displayName;
        private final String email;
    }

    @Getter
    @AllArgsConstructor
    public static class SpeciesInfo {
        private final UUID id;
        private final String commonNameKo;
        private final String commonNameEn;
        private final String scientificName;
        private final String status;
    }

    @Getter
    @AllArgsConstructor
    public static class MediaInfo {
        private final UUID id;
        private final String sanitizedUrl;      // 공개 이미지 URL (EXIF 제거됨)
        private final String storagePath;       // 원본 이미지 URL (소유자만)
        private final String mimeType;
        private final Long bytes;
        private final Integer width;
        private final Integer height;
        private final ExifInfo exif;            // EXIF 정보
    }

    @Getter
    @AllArgsConstructor
    public static class ExifInfo {
        private final String cameraMake;
        private final String cameraModel;
        private final String capturedAt;
        private final Double gpsLatitude;
        private final Double gpsLongitude;
    }
}
