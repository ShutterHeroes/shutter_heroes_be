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
     * @param callbackRequest YOLO 추론 결과
     * @return 200 OK
     */
    @PostMapping("/yolo/callback")
    @Operation(summary = "YOLO 추론 결과 Callback", description = "FastAPI YOLO 서버가 추론 완료 후 결과를 전송하는 엔드포인트")
    public ResponseEntity<Void> handleYoloCallback(@RequestBody YoloCallbackRequest callbackRequest) {
        log.info("Received YOLO callback: requestId={}, status={}",
            callbackRequest.getRequestId(), callbackRequest.getStatus());

        yoloCallbackService.processCallback(callbackRequest);

        return ResponseEntity.ok().build();
    }
}
