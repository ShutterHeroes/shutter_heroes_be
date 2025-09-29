package com.example.demo.domain.web.dto;

import com.example.demo.domain.enums.Visibility;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPQL constructor expression 호환을 위해 record 대신 클래스로 구현.
 * createdAt 타입 차이를 흡수하기 위해 오버로드 생성자를 제공한다.
 */
public class MediaBrowseItemDto {
    private UUID mediaId;
    private String storagePath;
    private String mimeType;
    private Integer width;
    private Integer height;
    private OffsetDateTime createdAt;  // 최종적으로 OffsetDateTime으로 normalize
    private UUID ownerId;
    private UUID sightingId;
    private Visibility sightingVisibility;

    // ---- Hibernate가 정확히 매칭할 수 있도록 여러 생성자 제공 ----

    // createdAt = OffsetDateTime 인 경우
    public MediaBrowseItemDto(UUID mediaId, String storagePath, String mimeType,
                              Integer width, Integer height, OffsetDateTime createdAt,
                              UUID ownerId, UUID sightingId, Visibility sightingVisibility) {
        this.mediaId = mediaId;
        this.storagePath = storagePath;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
        this.ownerId = ownerId;
        this.sightingId = sightingId;
        this.sightingVisibility = sightingVisibility;
    }

    // createdAt = LocalDateTime 인 경우
    public MediaBrowseItemDto(UUID mediaId, String storagePath, String mimeType,
                              Integer width, Integer height, LocalDateTime createdAt,
                              UUID ownerId, UUID sightingId, Visibility sightingVisibility) {
        this(mediaId, storagePath, mimeType, width, height,
                createdAt != null ? createdAt.atOffset(OffsetDateTime.now().getOffset()) : null,
                ownerId, sightingId, sightingVisibility);
    }

    // createdAt = Instant 인 경우
    public MediaBrowseItemDto(UUID mediaId, String storagePath, String mimeType,
                              Integer width, Integer height, Instant createdAt,
                              UUID ownerId, UUID sightingId, Visibility sightingVisibility) {
        this(mediaId, storagePath, mimeType, width, height,
                createdAt != null ? createdAt.atOffset(OffsetDateTime.now().getOffset()) : null,
                ownerId, sightingId, sightingVisibility);
    }

    // ===== Getter (직렬화용) =====
    public UUID getMediaId() { return mediaId; }
    public String getStoragePath() { return storagePath; }
    public String getMimeType() { return mimeType; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getSightingId() { return sightingId; }
    public Visibility getSightingVisibility() { return sightingVisibility; }
}
