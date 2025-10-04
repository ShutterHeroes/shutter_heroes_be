package com.example.demo.domain.dto.yolo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI YOLO 서버로부터 받는 Callback 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoloCallbackRequest {

    @JsonProperty("request_id")
    private String requestId;

    private String status;  // "success" or "error"

    private List<Result> results;

    @JsonProperty("error_message")
    private String errorMessage;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private String url;
        private List<Detection> detections;

        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detection {
        private String label;           // 클래스 레이블
        private Float confidence;       // 신뢰도 (0.0 ~ 1.0)
        private BoundingBox bbox;       // 바운딩 박스
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        @JsonProperty("x_min")
        private Float xMin;

        @JsonProperty("y_min")
        private Float yMin;

        @JsonProperty("x_max")
        private Float xMax;

        @JsonProperty("y_max")
        private Float yMax;
    }
}
