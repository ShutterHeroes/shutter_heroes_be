package com.example.demo.domain.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** 네이티브 쿼리 결과용 인터페이스 프로젝션 (alias ↔ getter 매칭) */
public interface SightingAroundRow {
    UUID getId();
    String getDisplayName();
    String getCommonNameKo();
    String getCommonNameEn();
    String getScientificName();
    String getStatus();
    String getStoragePath();
    String getTitle();
    String getDescription();
    LocalDateTime getOccurredAt();
    String getDetectedBy();
    Double getAiConfidence();
    String getVisibility();
    Boolean getIsVerified();
    String getGeom();          // GeoJSON
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
