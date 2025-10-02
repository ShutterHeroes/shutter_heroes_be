package com.example.demo.domain.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 반경 radiusMeter(지도 확대 축소에 따라) 내 sighting 조회 결과 아이템
 * geom은 GeoJSON 문자열로 반환 (드라이버 의존성 없이 안전)
 */
@Getter
@AllArgsConstructor
public class SightingAroundItemDto {
    private final UUID id;                 // sightings.id
    private final String displayName;      // users.display_name
    private final String commonNameKo;     // species.common_name_ko
    private final String commonNameEn;     // species.common_name_en
    private final String scientificName;   // species.scientific_name
    private final String status;           // species.status::text
    private final String storagePath;      // media.storage_path
    private final String title;            // s.title
    private final String description;      // s.description
    private final LocalDateTime occurredAt;// s.occurred_at
    private final String detectedBy;       // s.detected_by::text
    private final Double aiConfidence;     // s.ai_confidence
    private final String visibility;       // s.visibility::text ('public'|'private')
    private final Boolean isVerified;      // s.is_verified
    private final String geom;             // ST_AsGeoJSON(s.geom)
    private final LocalDateTime createdAt; // s.created_at
    private final LocalDateTime updatedAt; // s.updated_at
}
