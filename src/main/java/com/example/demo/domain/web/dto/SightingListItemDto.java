package com.example.demo.domain.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sighting 목록 조회 결과 아이템 DTO
 *
 * <p>페이징 처리된 목록에서 사용되며, 간략한 정보만 포함합니다.</p>
 * <p>geom은 GeoJSON 문자열로 반환됩니다.</p>
 */
@Getter
@AllArgsConstructor
public class SightingListItemDto {
    // Sighting 기본 정보
    private final UUID id;
    private final String title;
    private final String description;
    private final LocalDateTime occurredAt;
    private final String detectedBy;           // 'AI' | 'USER'
    private final BigDecimal aiConfidence;
    private final String visibility;           // 'public' | 'private'
    private final Boolean isVerified;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // User 정보
    private final String displayName;          // users.display_name

    // Species 정보
    private final String commonNameKo;         // species.common_name_ko
    private final String commonNameEn;         // species.common_name_en
    private final String scientificName;       // species.scientific_name
    private final String status;               // species.status::text

    // Media 정보
    private final String sanitizedUrl;         // 공개 이미지 URL (EXIF 제거됨)

    // GPS 정보
    private final String geom;                 // ST_AsGeoJSON(s.geom)
}
