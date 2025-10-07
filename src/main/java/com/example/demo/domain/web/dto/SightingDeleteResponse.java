package com.example.demo.domain.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Sighting 삭제 응답 DTO
 */
@Getter
@AllArgsConstructor
public class SightingDeleteResponse {
    private final UUID id;
    private final String message;

    public static SightingDeleteResponse of(UUID id) {
        return new SightingDeleteResponse(
            id,
            "Sighting deleted successfully"
        );
    }
}
