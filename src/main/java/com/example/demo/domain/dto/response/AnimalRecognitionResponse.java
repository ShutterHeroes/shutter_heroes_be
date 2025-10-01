package com.example.demo.domain.dto.response;

import com.example.demo.domain.dto.vision.AnimalDetection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalRecognitionResponse {

    private String imageFileName;
    private List<AnimalDetection> detections;
    private Integer totalDetections;
    private Float averageConfidence;
    private LocalDateTime timestamp;

    public static AnimalRecognitionResponse of(String imageFileName, List<AnimalDetection> detections) {
        Float avgConfidence = detections.stream()
            .map(AnimalDetection::getConfidence)
            .reduce(0.0f, Float::sum) / detections.size();

        return AnimalRecognitionResponse.builder()
            .imageFileName(imageFileName)
            .detections(detections)
            .totalDetections(detections.size())
            .averageConfidence(avgConfidence)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
