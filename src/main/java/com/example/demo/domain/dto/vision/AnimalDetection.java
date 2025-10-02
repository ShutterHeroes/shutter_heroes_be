package com.example.demo.domain.dto.vision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalDetection {

    private String label;              // 일반 명칭 (예: "Cat", "Dog", "Bengal Cat")
    private Float confidence;          // 신뢰도 (0.0 ~ 1.0)
    private String description;        // 설명
    private String scientificName;     // 학명 (예: "Felis catus")
    private BoundingBox boundingBox;   // 이미지 내 위치 정보 (선택 사항)

    public static AnimalDetection of(String label, Float confidence, String description) {
        return AnimalDetection.builder()
            .label(label)
            .confidence(confidence)
            .description(description)
            .build();
    }

    public static AnimalDetection of(String label, Float confidence, String description, BoundingBox boundingBox) {
        return AnimalDetection.builder()
            .label(label)
            .confidence(confidence)
            .description(description)
            .boundingBox(boundingBox)
            .build();
    }

    public static AnimalDetection of(String label, Float confidence, String description, String scientificName) {
        return AnimalDetection.builder()
            .label(label)
            .confidence(confidence)
            .description(description)
            .scientificName(scientificName)
            .build();
    }

    public static AnimalDetection of(String label, Float confidence, String description, String scientificName, BoundingBox boundingBox) {
        return AnimalDetection.builder()
            .label(label)
            .confidence(confidence)
            .description(description)
            .scientificName(scientificName)
            .boundingBox(boundingBox)
            .build();
    }
}
