package com.example.demo.domain.web;

import com.example.demo.domain.dto.yolo.YoloCallbackRequest;
import com.example.demo.domain.service.YoloCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FastAPI YOLO 서버로부터 추론 결과를 받는 Callback 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "YOLO Callback", description = "FastAPI YOLO 추론 결과 Callback API")
public class YoloCallbackController {

    private final YoloCallbackService yoloCallbackService;

    /**
     * FastAPI YOLO 서버로부터 추론 결과를 받는 Callback 엔드포인트
     *
     * String rawJson 형태로 수신하여 로그에 기록하고, YoloCallbackRequest로 파싱을 시도합니다.
     * @return 200 OK
     */
    @PostMapping("/yolo/callback")
    @Operation(summary = "YOLO 추론 결과 Callback", description = "FastAPI YOLO 서버가 추론 완료 후 결과를 전송하는 엔드포인트")
    public ResponseEntity<Void> handleYoloCallback(@RequestBody String rawJson) {
        log.info("Received YOLO callback (raw JSON): {}", rawJson);

        try {
            // JSON 파싱 시도
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            YoloCallbackRequest callbackRequest = objectMapper.readValue(rawJson, YoloCallbackRequest.class);

            log.info("Parsed YOLO callback: requestId={}, status={}, results={}, errorMessage={}",
                callbackRequest.getRequestId(),
                callbackRequest.getStatus(),
                callbackRequest.getResults() != null ? callbackRequest.getResults().size() : 0,
                callbackRequest.getErrorMessage());

            yoloCallbackService.processCallback(callbackRequest);
        } catch (Exception e) {
            log.error("Failed to parse YOLO callback JSON: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }
}
