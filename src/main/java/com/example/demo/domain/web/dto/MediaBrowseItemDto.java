package com.example.demo.domain.web.dto;

import com.example.demo.domain.enums.Visibility;
// 필요 시 null 필드 숨기고 싶다면 주석 해제하세요.
// import com.fasterxml.jackson.annotation.JsonInclude;
// @JsonInclude(JsonInclude.Include.NON_NULL)
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.*;
import java.util.UUID;

/**
 * JPQL constructor expression 호환 DTO.
 * - @Getter / @AllArgsConstructor 로 보일러플레이트 최소화
 * - createdAt 타입(LocalDateTime/Instant/OffsetDateTime) 차이를 흡수하는 오버로드 생성자 제공
 */
@Getter
@AllArgsConstructor
public class MediaBrowseItemDto {
    private final UUID mediaId;
    private final String storagePath;
    private final String mimeType;
    private final Integer width;
    private final Integer height;
    private final OffsetDateTime createdAt;   // 표준화
    private final UUID ownerId;
    private final UUID sightingId;            // /my에서는 null 가능
    private final Visibility sightingVisibility;

    // JPQL이 LocalDateTime을 넘길 때 매칭
    public MediaBrowseItemDto(UUID mediaId, String storagePath, String mimeType,
                              Integer width, Integer height, LocalDateTime createdAt,
                              UUID ownerId, UUID sightingId, Visibility sightingVisibility) {
        this(mediaId, storagePath, mimeType, width, height,
            toOffset(createdAt), ownerId, sightingId, sightingVisibility);
    }

    // JPQL이 Instant를 넘길 때 매칭
    public MediaBrowseItemDto(UUID mediaId, String storagePath, String mimeType,
                              Integer width, Integer height, Instant createdAt,
                              UUID ownerId, UUID sightingId, Visibility sightingVisibility) {
        this(mediaId, storagePath, mimeType, width, height,
            toOffset(createdAt), ownerId, sightingId, sightingVisibility);
    }

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        if (ldt == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        return ldt.atZone(zone).toOffsetDateTime();
    }

    private static OffsetDateTime toOffset(Instant inst) {
        if (inst == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        return inst.atZone(zone).toOffsetDateTime();
    }
}
