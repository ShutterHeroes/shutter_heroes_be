package com.example.demo.domain.web;

import com.example.demo.config.openai.OpenAiConfig;
import com.example.demo.domain.dto.response.AnimalDescriptionResponse;
import com.example.demo.domain.service.OpenAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "동물 학명 기반 설명 제공 API")
public class AiController {

    private final OpenAiService openAiService;
    private final OpenAiConfig openAiConfig;

    @PostMapping("/animal/description")
    @Operation(
        summary = "동물 설명 조회",
        description = "동물의 학명을 입력받아 해당 동물에 대한 상세한 설명을 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "동물 설명 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (학명 형식 오류)"),
        @ApiResponse(responseCode = "500", description = "OpenAI API 호출 실패")
    })
    public Mono<AnimalDescriptionResponse> getAnimalDescription(
        @RequestParam @NotBlank(message = "학명은 필수입니다") String scientificName
    ) {
        return openAiService.getAnimalDescription(scientificName)
            .map(description -> AnimalDescriptionResponse.of(scientificName, description));
    }

    @GetMapping("/config")
    @Operation(
        summary = "AI 설정 정보 조회",
        description = "현재 OpenAI API 설정 정보를 조회합니다. (API 키는 마스킹됨)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 정보 조회 성공")
    })
    public Mono<Object> getConfig() {
        return Mono.just(new Object() {
            public final String model = openAiConfig.getModel();
            public final String apiUrl = openAiConfig.getApiUrl();
            public final String apiKey = maskApiKey(openAiConfig.getApiKey());
        });
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            return "***";
        }
        return apiKey.substring(0, 7) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}

