package com.example.demo.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalDescriptionResponse {

    private String scientificName;
    private String description;
    private LocalDateTime timestamp;

    public static AnimalDescriptionResponse of(String scientificName, String description) {
        return AnimalDescriptionResponse.builder()
            .scientificName(scientificName)
            .description(description)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
