package com.example.demo.domain.dto.yolo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * FastAPI YOLO 서버로부터 받는 Callback 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoloCallbackRequest {

    @JsonProperty("request_id")
    private String requestId;

    private String status;  // "success" or "error" (optional)

    private List<Result> results;

    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * status 필드가 없으면 results 존재 여부로 성공 여부 판단
     */
    public String getStatus() {
        if (status != null) {
            return status;
        }
        // status가 없으면 results가 있고 비어있지 않으면 "success"
        if (results != null && !results.isEmpty()) {
            return "success";
        }
        return "error";
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String source;  // FastAPI는 "source" 필드 사용
        private String url;     // 호환성을 위해 유지

        @JsonProperty("result")
        private ResultDetail resultDetail;

        @JsonProperty("error_message")
        private String errorMessage;

        // source 또는 url 중 하나 반환
        public String getImageUrl() {
            return source != null ? source : url;
        }

        // Detection 리스트를 preds에서 가져옴
        public List<Detection> getDetections() {
            if (resultDetail != null && resultDetail.getPreds() != null) {
                return resultDetail.getPreds();
            }
            return List.of();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultDetail {
        private String task;

        @JsonProperty("speed_ms")
        private Map<String, Double> speedMs;

        private Map<String, Object> probs;

        private List<Detection> preds;  // 이전 버전 호환성

        private List<Detection> detections;  // 새 버전 YOLO 응답 형식

        // preds 또는 detections 중 하나 반환
        public List<Detection> getPreds() {
            if (detections != null && !detections.isEmpty()) {
                return detections;
            }
            return preds != null ? preds : List.of();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Detection {
        @JsonProperty("class_id")
        private Integer classId;

        private String label;           // 클래스 레이블

        private Float score;            // FastAPI는 "score" 필드 사용
        private Float confidence;       // 호환성을 위해 유지

        private BoundingBox bbox;       // 바운딩 박스 (구버전)

        @JsonProperty("bbox_xyxy")
        private List<Float> bboxXyxy;   // 새 버전 바운딩 박스 [x_min, y_min, x_max, y_max]

        // score 또는 confidence 중 하나 반환
        public Float getConfidence() {
            return score != null ? score : confidence;
        }

        // 바운딩 박스 반환 (bbox_xyxy를 BoundingBox로 변환 또는 기존 bbox 사용)
        public BoundingBox getBbox() {
            if (bboxXyxy != null && bboxXyxy.size() == 4) {
                return BoundingBox.builder()
                    .xMin(bboxXyxy.get(0))
                    .yMin(bboxXyxy.get(1))
                    .xMax(bboxXyxy.get(2))
                    .yMax(bboxXyxy.get(3))
                    .build();
            }
            return bbox;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
