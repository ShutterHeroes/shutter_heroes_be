package com.example.demo.domain.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** 네이티브 쿼리 결과를 받는 인터페이스 프로젝션 (컬럼 alias와 메서드명이 매칭되어야 함) */
public interface MediaBrowseRow {
    UUID getMediaId();
    String getStoragePath();
    String getMimeType();
    Integer getWidth();
    Integer getHeight();
    LocalDateTime getCreatedAt();   // TIMESTAMP → LocalDateTime 매핑
    UUID getOwnerId();
    UUID getSightingId();
    String getSightingVisibility(); // 'public' / 'private' 그대로 받음
}
