package com.example.demo.domain.service;

import com.example.demo.config.openai.OpenAiConfig;
import com.example.demo.domain.dto.openai.ChatCompletionRequest;
import com.example.demo.domain.dto.openai.ChatCompletionResponse;
import com.example.demo.domain.dto.openai.ChatMessage;
import com.example.demo.exceptions.errorcode.AiErrorCode;
import com.example.demo.exceptions.exception.AiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient openAiWebClient;
    private final OpenAiConfig openAiConfig;

    /**
     * 동물의 학명을 입력받아 해당 동물에 대한 상세 설명을 반환합니다.
     *
     * @param scientificName 동물의 학명 (예: "Panthera leo")
     * @return 동물에 대한 상세 설명 (한국어)
     */
    public Mono<String> getAnimalDescription(String scientificName) {
        log.info("Requesting animal description for scientific name: {}", scientificName);

        // 학명 유효성 검증
        validateScientificName(scientificName);

        String prompt = buildAnimalDescriptionPrompt(scientificName);

        List<ChatMessage> messages = Arrays.asList(
            ChatMessage.system("당신은 동물학 전문가입니다. 제공된 학명을 가진 동물에 대해 정확하고 상세한 정보를 한국어로 제공해주세요."),
            ChatMessage.user(prompt)
        );

        ChatCompletionRequest request = ChatCompletionRequest.of(
            openAiConfig.getModel(),
            messages,
            null,
            null
        );

        return chatCompletion(request)
            .map(this::validateResponse)
            .doOnSuccess(response -> log.info("Animal description retrieved successfully for: {}", scientificName))
            .doOnError(error -> log.error("Failed to get animal description for {}: {}", scientificName, error.getMessage()));
    }

    private Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request) {
        return openAiWebClient
            .post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatCompletionResponse.class)
            .timeout(Duration.ofSeconds(120)) // Reactive stream timeout 120초
            .doOnSubscribe(subscription -> log.info("OpenAI request subscribed, waiting for response..."))
            .doOnSuccess(response -> log.info("OpenAI chat completion successful"))
            .doOnError(error -> log.error("OpenAI chat completion failed: {}", error.getMessage()))
            .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> {
                log.error("Mapping TimeoutException to AiException");
                return new AiException(AiErrorCode.OPENAI_API_TIMEOUT);
            })
            .onErrorMap(WebClientResponseException.class, this::handleWebClientException);
    }

    private String buildAnimalDescriptionPrompt(String scientificName) {
        return String.format("""
            학명이 '%s'인 동물에 대해 다음 정보를 제공해주세요:

            1. 한국명 (일반적으로 불리는 이름)
            2. 영문명 (영어 이름)
            3. 분류학적 정보 (과, 목, 강 등)
            4. 신체적 특징 (크기, 외형, 색깔 등)
            5. 서식지 및 분포 지역
            6. 생활 습성 (먹이, 행동 패턴, 번식 등)
            7. 보존 상태 (멸종위기 여부, IUCN 등급 등)
            8. 생태계에서의 역할
            9. 인간과의 관계 (문화적 의미, 경제적 가치 등)
            10. 흥미로운 사실이나 특이점

            만약 제공된 학명이 정확하지 않거나 존재하지 않는 경우, 그 사실을 명시하고 가장 유사한 동물을 제안해주세요.
            모든 정보는 한국어로 작성하고, 정확하고 교육적인 내용으로 구성해주세요. 정보는 json text 형식으로 반환해주세요.
            예시 = {"한국명":"종다리","영문명":"Eurasian Skylark"}
            """, scientificName);
    }

    private void validateScientificName(String scientificName) {
        if (!StringUtils.hasText(scientificName)) {
            throw new AiException(AiErrorCode.INVALID_SCIENTIFIC_NAME);
        }

        // 기본적인 학명 형식 검증 (속명 종명 형태)
        String trimmed = scientificName.trim();
        if (trimmed.length() < 3 || !trimmed.matches("^[A-Za-z]+\\s+[A-Za-z]+.*$")) {
            throw new AiException(AiErrorCode.INVALID_SCIENTIFIC_NAME);
        }
    }

    private String validateResponse(ChatCompletionResponse response) {
        String content = response.getContent();
        if (!StringUtils.hasText(content)) {
            throw new AiException(AiErrorCode.EMPTY_RESPONSE);
        }
        return content;
    }

    private AiException handleWebClientException(WebClientResponseException ex) {
        log.error("OpenAI API error - Status: {}, Response: {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return switch (status) {
            case UNAUTHORIZED -> new AiException(AiErrorCode.OPENAI_API_UNAUTHORIZED);
            case TOO_MANY_REQUESTS -> new AiException(AiErrorCode.OPENAI_API_RATE_LIMIT);
            case PAYMENT_REQUIRED -> new AiException(AiErrorCode.OPENAI_API_QUOTA_EXCEEDED);
            default -> new AiException(AiErrorCode.OPENAI_API_ERROR);
        };
    }
}
