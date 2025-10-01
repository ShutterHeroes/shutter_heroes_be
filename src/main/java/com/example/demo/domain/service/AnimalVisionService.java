package com.example.demo.domain.service;

import com.example.demo.domain.dto.vision.AnimalDetection;
import com.example.demo.exceptions.errorcode.AiErrorCode;
import com.example.demo.exceptions.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI GPT-4o Vision API를 사용한 동물 인식 서비스
 *
 * <p>이미지에서 동물을 인식하고 정확한 학명을 분류합니다.</p>
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>이미지에서 여러 동물을 동시에 인식</li>
 *   <li>동물의 정확한 학명 반환</li>
 *   <li>신뢰도 기반 필터링</li>
 *   <li>구체적인 종 구분 (예: Bengal Cat vs Persian Cat)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalVisionService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    /**
     * OpenAI GPT-4o Vision API를 사용하여 이미지에서 여러 동물을 인식하고 학명을 반환합니다.
     *
     * <p><b>동작 원리:</b></p>
     * <ol>
     *   <li>이미지를 Base64로 인코딩</li>
     *   <li>GPT-4o Vision API에 이미지 전송 (maxResults 전달)</li>
     *   <li>응답받은 JSON 배열 파싱 (animals: [{ label, scientificName, confidence }, ...])</li>
     *   <li>신뢰도 임계값 적용하여 필터링</li>
     * </ol>
     *
     * <p><b>GPT-4o Vision API 특징:</b></p>
     * <ul>
     *   <li>이미지를 직접 분석하여 정확한 종 구분 (예: Bengal Cat vs Persian Cat)</li>
     *   <li>학명을 바로 반환 (1회 API 호출로 완료)</li>
     *   <li>여러 동물을 동시에 인식 가능</li>
     *   <li>높은 정확도와 신뢰도</li>
     * </ul>
     *
     * @param imageFile 분석할 이미지 파일 (MultipartFile 형식, 최대 10MB)
     * @param confidenceThreshold 신뢰도 임계값 0.0~1.0 (null이면 기본값 0.5 사용)
     * @param maxResults 최대 결과 개수 (null이면 기본값 10개)
     * @return 동물 탐지 결과 리스트 (신뢰도 높은 순으로 정렬)
     * @throws AiException 이미지 처리 실패 또는 Vision API 오류 발생 시
     */
    public List<AnimalDetection> detectAnimals(MultipartFile imageFile, Float confidenceThreshold, Integer maxResults) {
        log.info("Starting animal detection with GPT-4o Vision for image: {} (maxResults: {})",
            imageFile.getOriginalFilename(), maxResults);

        try {
            // 1. 이미지 파일 검증
            validateImageFile(imageFile);

            // 2. 신뢰도 임계값 설정 (기본값: 0.5)
            float threshold = confidenceThreshold != null ? confidenceThreshold : 0.5f;

            // 3. maxResults 기본값 설정 (기본값: 10)
            int maxCount = maxResults != null ? maxResults : 10;

            // 4. OpenAI Vision API 호출 (동기 처리)
            String jsonResponse = openAiService.analyzeAnimalImage(imageFile, maxCount)
                .block();  // Mono를 동기적으로 처리

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                log.warn("Empty response from Vision API");
                return new ArrayList<>();
            }

            // 5. JSON 응답 파싱 (배열 처리)
            JsonNode responseNode = objectMapper.readTree(jsonResponse);
            JsonNode animalsArray = responseNode.get("animals");

            if (animalsArray == null || !animalsArray.isArray()) {
                log.warn("Invalid response format: 'animals' array not found");
                return new ArrayList<>();
            }

            // 6. 배열을 순회하며 AnimalDetection 리스트 생성
            List<AnimalDetection> detections = new ArrayList<>();
            for (JsonNode animalNode : animalsArray) {
                String label = animalNode.get("label").asText();
                String scientificName = animalNode.get("scientificName").asText();
                float confidence = (float) animalNode.get("confidence").asDouble();

                // 7. 신뢰도 임계값 필터링
                if (confidence >= threshold && !label.equals("해당 없음")) {
                    AnimalDetection detection = AnimalDetection.of(
                        label,                    // 일반 명칭 (예: "Bengal Cat")
                        confidence,               // 신뢰도 (0.0 ~ 1.0)
                        label,                    // 설명 (label과 동일)
                        scientificName            // 학명 (예: "Felis catus")
                    );
                    detections.add(detection);
                }
            }

            log.info("Animal detection completed: {} animals detected (threshold: {})",
                detections.size(), threshold);

            return detections;

        } catch (Exception e) {
            log.error("Unexpected error during animal detection: {}", e.getMessage(), e);
            throw new AiException(AiErrorCode.VISION_API_ERROR);
        }
    }

    /**
     * 이미지 파일 유효성 검증
     *
     * <p>검증 항목:</p>
     * <ul>
     *   <li>파일이 비어있지 않은지 확인</li>
     *   <li>파일 형식이 이미지인지 확인 (MIME type)</li>
     *   <li>파일 크기가 제한(10MB) 이내인지 확인</li>
     * </ul>
     *
     * @param imageFile 검증할 이미지 파일
     * @throws AiException 파일이 유효하지 않을 경우
     */
    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new AiException(AiErrorCode.INVALID_IMAGE_FILE);
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AiException(AiErrorCode.INVALID_IMAGE_FORMAT);
        }

        // 파일 크기 제한 (10MB)
        long maxFileSize = 10 * 1024 * 1024; // 10MB in bytes
        if (imageFile.getSize() > maxFileSize) {
            throw new AiException(AiErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        log.debug("Image file validation passed: {} ({})", imageFile.getOriginalFilename(), contentType);
    }
}
