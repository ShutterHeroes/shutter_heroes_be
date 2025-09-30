package com.example.demo.domain.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "sighting 가시성 변경 응답")
public class UpdateVisibilityResponse {
    private final boolean success;
    private final int updatedSightings;
    private final String visibility; // normalized: 'public' | 'private'
    private final String message;

    public UpdateVisibilityResponse(boolean success, int updatedSightings, String visibility, String message) {
        this.success = success;
        this.updatedSightings = updatedSightings;
        this.visibility = visibility;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public int getUpdatedSightings() { return updatedSightings; }
    public String getVisibility() { return visibility; }
    public String getMessage() { return message; }

    public static UpdateVisibilityResponse of(int updated, String visibility, String message) {
        return new UpdateVisibilityResponse(true, updated, visibility, message);
    }
}
