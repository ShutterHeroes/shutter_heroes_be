package com.example.demo.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalRecognitionRequest {

    @NotNull(message = "이미지 파일은 필수입니다")
    private MultipartFile image;

    private Float confidenceThreshold;
    private Integer maxResults;
    private Boolean includeSpeciesInfo;

    public static AnimalRecognitionRequest of(MultipartFile image) {
        return AnimalRecognitionRequest.builder()
            .image(image)
            .confidenceThreshold(0.5f)
            .maxResults(10)
            .includeSpeciesInfo(true)
            .build();
    }
}
