package com.example.demo.domain.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "sighting 가시성 변경 요청")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVisibilityRequest {
    @Schema(description = "목표 가시성: public 또는 private (대소문자 무시)", example = "public")
    private String visibility;
}
