package com.example.demo.domain.service;

import com.example.demo.domain.dto.yolo.YoloInferRequest;
import com.example.demo.domain.dto.yolo.YoloInferResponse;
import com.example.demo.exceptions.errorcode.AiErrorCode;
import com.example.demo.exceptions.exception.AiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * FastAPI YOLO 추론 서버와 통신하는 서비스
 *
 * <p>FastAPI 서버에 이미지 URL을 전송하고 비동기로 동물 탐지 결과를 받습니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoloInferenceService {

    private final WebClient.Builder webClientBuilder;

    @Value("${yolo.inference.url:http://localhost:8000}")
    private String yoloInferenceUrl;

    @Value("${yolo.inference.timeout:10}")
    private int timeoutSeconds;

    @Value("${yolo.inference.callback-base-url:http://localhost:8080}")
    private String callbackBaseUrl;

    /**
     * FastAPI YOLO 서버에 추론 요청을 보냅니다.
     *
     * @param imageUrls S3 이미지 URL 리스트
     * @return YoloInferResponse (request_id, status)
     */
    public YoloInferResponse requestInference(List<String> imageUrls) {
        String callbackUrl = callbackBaseUrl + "/api/v1/yolo/callback";
        log.info("Requesting YOLO inference for {} images, callback: {}", imageUrls.size(), callbackUrl);

        YoloInferRequest request = YoloInferRequest.of(imageUrls, callbackUrl);

        try {
            WebClient webClient = webClientBuilder
                .baseUrl(yoloInferenceUrl)
                .build();

            YoloInferResponse response = webClient.post()
                .uri("/infer")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(YoloInferResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            if (response == null) {
                log.error("Received null response from YOLO inference server");
                throw new AiException(AiErrorCode.VISION_API_ERROR);
            }

            log.info("YOLO inference request accepted: requestId={}, status={}",
                response.getRequestId(), response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Failed to request YOLO inference: {}", e.getMessage(), e);
            throw new AiException(AiErrorCode.VISION_API_ERROR);
        }
    }
}
