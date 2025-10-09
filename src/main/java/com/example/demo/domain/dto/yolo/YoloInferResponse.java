package com.example.demo.domain.dto.yolo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI YOLO 서버로부터 받는 추론 요청 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoloInferResponse {

    @JsonProperty("request_id")
    private String requestId;

    private String status;
}
