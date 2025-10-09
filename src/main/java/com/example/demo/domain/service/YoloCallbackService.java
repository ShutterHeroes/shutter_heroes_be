package com.example.demo.domain.service;

import com.example.demo.domain.dto.yolo.YoloCallbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YOLO Callback 결과를 처리하는 서비스
 *
 * <p>FastAPI 서버로부터 받은 추론 결과를 임시 저장하고 관리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoloCallbackService {

    // requestId -> YoloCallbackRequest 매핑 (임시 저장)
    private final Map<String, YoloCallbackRequest> callbackResults = new ConcurrentHashMap<>();

    /**
     * FastAPI로부터 받은 Callback 결과를 저장합니다.
     *
     * @param callbackRequest YOLO 추론 결과
     */
    public void processCallback(YoloCallbackRequest callbackRequest) {
        String requestId = callbackRequest.getRequestId();

        if ("success".equals(callbackRequest.getStatus())) {
            int detectionCount = 0;
            if (callbackRequest.getResults() != null && !callbackRequest.getResults().isEmpty()) {
                detectionCount = callbackRequest.getResults().get(0).getDetections().size();
            }

            log.info("YOLO inference succeeded for requestId: {}, detections: {}, status: {}",
                requestId, detectionCount, callbackRequest.getStatus());

            // 첫 번째 detection 정보 로깅
            if (detectionCount > 0) {
                YoloCallbackRequest.Detection topDetection = callbackRequest.getResults().get(0).getDetections().get(0);
                log.info("YOLO top detection: label={}, score={}", topDetection.getLabel(), topDetection.getConfidence());
            }
        } else {
            log.error("YOLO inference failed for requestId: {}, status: {}, error: {}",
                requestId, callbackRequest.getStatus(), callbackRequest.getErrorMessage());
        }

        // 결과 저장
        callbackResults.put(requestId, callbackRequest);
    }

    /**
     * requestId에 해당하는 YOLO 결과를 조회합니다.
     *
     * @param requestId YOLO 요청 ID
     * @return YoloCallbackRequest (없으면 null)
     */
    public YoloCallbackRequest getResult(String requestId) {
        return callbackResults.get(requestId);
    }

    /**
     * requestId에 해당하는 YOLO 결과를 제거합니다.
     *
     * @param requestId YOLO 요청 ID
     */
    public void removeResult(String requestId) {
        callbackResults.remove(requestId);
        log.debug("Removed YOLO result for requestId: {}", requestId);
    }
}
