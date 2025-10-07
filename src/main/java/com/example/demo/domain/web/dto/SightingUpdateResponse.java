package com.example.demo.domain.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sighting 수정 응답 DTO
 */
@Getter
@AllArgsConstructor
public class SightingUpdateResponse {
    private final UUID id;
    private final String title;
    private final String description;
    private final String visibility;
    private final LocalDateTime occurredAt;
    private final String addressText;
    private final LocalDateTime updatedAt;
    private final String message;

    public static SightingUpdateResponse of(
        UUID id,
        String title,
        String description,
        String visibility,
        LocalDateTime occurredAt,
        String addressText,
        LocalDateTime updatedAt
    ) {
        return new SightingUpdateResponse(
            id,
            title,
            description,
            visibility,
            occurredAt,
            addressText,
            updatedAt,
            "Sighting updated successfully"
        );
    }
}
