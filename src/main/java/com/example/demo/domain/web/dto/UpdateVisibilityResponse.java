// C:\github\shutter_heroes_be\src\main\java\com\example\demo\domain\web\dto\UpdateVisibilityResponse.java
package com.example.demo.domain.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "sighting 가시성 변경 응답")
@Getter
@AllArgsConstructor
@Builder
@ToString
public class UpdateVisibilityResponse {
    private final boolean success;
    private final int updatedSightings;
    /** normalized: 'public' | 'private' */
    private final String visibility;
    private final String message;

    public static UpdateVisibilityResponse of(int updated, String visibility, String message) {
        return new UpdateVisibilityResponse(true, updated, visibility, message);
    }
}
