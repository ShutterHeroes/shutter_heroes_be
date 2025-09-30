package com.example.demo.domain.service;

import com.example.demo.domain.dto.openai.OpenAiAnimalInfoDto;
import com.example.demo.domain.dto.response.AnimalDescriptionResponse;
import com.example.demo.domain.entity.Species;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * AI 관련 비즈니스 로직을 처리하는 서비스
 * Controller와 OpenAiService, SpeciesService 사이의 조정자 역할
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final SpeciesService speciesService;
    private final OpenAiService openAiService;

    /**
     * 동물 설명 조회 (캐시 우선, 없으면 OpenAI 호출)
     * <p>
     * 비즈니스 로직:
     * 1. DB에서 학명으로 Species 조회
     * 2. 존재하면 (캐시 히트): notes의 JSON을 파싱하여 반환
     * 3. 없으면 (캐시 미스): OpenAI 호출 → DB 저장 → 파싱하여 반환
     *
     * @param scientificName 동물의 학명
     * @return AnimalDescriptionResponse (fromCache 플래그 포함)
     */
    public Mono<AnimalDescriptionResponse> getAnimalDescription(String scientificName) {
        log.info("Processing animal description request for: {}", scientificName);

        // 1. DB 조회 시도
        return speciesService.findByScientificName(scientificName)
            .flatMap(species -> {
                // 캐시 히트: DB에서 조회한 Species를 응답으로 변환
                log.info("Cache hit for species: {}", scientificName);
                return buildResponseFromSpecies(species, true);
            })
            .switchIfEmpty(
                // 캐시 미스: OpenAI 호출하여 Species 생성 후 응답으로 변환
                Mono.defer(() -> {
                    log.info("Cache miss for species: {}. Fetching from OpenAI and saving.", scientificName);
                    return speciesService.getOrCreateSpecies(scientificName)
                        .flatMap(species -> buildResponseFromSpecies(species, false));
                })
            );
    }

    /**
     * Species 엔티티를 AnimalDescriptionResponse로 변환
     * notes 필드의 JSON을 파싱하여 detailInfo에 포함
     *
     * @param species Species 엔티티
     * @param fromCache 캐시에서 가져왔는지 여부
     * @return Mono<AnimalDescriptionResponse>
     */
    private Mono<AnimalDescriptionResponse> buildResponseFromSpecies(Species species, boolean fromCache) {
        return Mono.fromCallable(() -> {
            // notes의 JSON을 파싱
            OpenAiAnimalInfoDto detailInfo = openAiService.parseAnimalInfo(species.getNotes());

            // 응답 생성
            return AnimalDescriptionResponse.of(species, fromCache, detailInfo);
        });
    }
}
