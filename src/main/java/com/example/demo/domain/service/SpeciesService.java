package com.example.demo.domain.service;

import com.example.demo.domain.dto.openai.OpenAiAnimalInfoDto;
import com.example.demo.domain.entity.Species;
import com.example.demo.domain.enums.SpeciesStatus;
import com.example.demo.domain.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Species 엔티티 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final OpenAiService openAiService;

    /**
     * 학명으로 Species 조회 또는 생성
     * 1. DB에서 학명으로 조회
     * 2. 존재하면 반환
     * 3. 없으면 OpenAI API 호출하여 정보 수집
     * 4. Species 엔티티 생성 및 저장 (JSON 원본 그대로 저장)
     * 5. 저장된 엔티티 반환
     *
     * @param scientificName 학명 (예: "Panthera leo")
     * @return Mono<Species>
     */
    @Transactional
    public Mono<Species> getOrCreateSpecies(String scientificName) {
        log.info("Getting or creating species for scientific name: {}", scientificName);

        // 1. DB에서 조회
        return Mono.fromCallable(() -> speciesRepository.findByScientificName(scientificName))
            .flatMap(optionalSpecies -> {
                if (optionalSpecies.isPresent()) {
                    log.info("Species found in database: {}", scientificName);
                    return Mono.just(optionalSpecies.get());
                } else {
                    log.info("Species not found in database. Fetching from OpenAI: {}", scientificName);
                    // 2. DB에 없으면 OpenAI 호출 (JSON 문자열로 받음)
                    return openAiService.getAnimalDescriptionJson(scientificName)
                        .flatMap(jsonResponse -> saveSpeciesFromJson(scientificName, jsonResponse));
                }
            });
    }

    /**
     * OpenAI JSON 응답을 기반으로 Species 엔티티 생성 및 저장
     * JSON을 원본 그대로 notes에 저장하고, 필요한 필드만 파싱
     *
     * @param scientificName 학명
     * @param jsonResponse OpenAI로부터 받은 JSON 문자열
     * @return Mono<Species>
     */
    private Mono<Species> saveSpeciesFromJson(String scientificName, String jsonResponse) {
        return Mono.fromCallable(() -> {
            // JSON 파싱하여 필요한 정보 추출
            OpenAiAnimalInfoDto animalInfo = openAiService.parseAnimalInfo(jsonResponse);

            Species species = new Species();
            species.setScientificName(scientificName);
            species.setCommonNameKo(animalInfo.getCommonNameKo());
            species.setCommonNameEn(animalInfo.getCommonNameEn());

            // 보존 상태 파싱
            species.setStatus(parseConservationStatus(animalInfo.getConservationStatus()));

            // notes에 JSON 원본 그대로 저장
            species.setNotes(jsonResponse);

            Species savedSpecies = speciesRepository.save(species);
            log.info("Species saved to database: {} (ID: {})", scientificName, savedSpecies.getId());
            return savedSpecies;
        });
    }

    /**
     * 보존 상태 문자열을 SpeciesStatus enum으로 변환
     *
     * @param conservationStatus 보존 상태 문자열
     * @return SpeciesStatus
     */
    private SpeciesStatus parseConservationStatus(String conservationStatus) {
        if (conservationStatus == null) {
            return SpeciesStatus.GENERAL;
        }

        String status = conservationStatus.toUpperCase();
        if (status.contains("NATURAL_MONUMENT") || status.contains("천연") || status.contains("천연기념물")) {
            return SpeciesStatus.NATURAL_MONUMENT;
        } else if (status.contains("ENDANGERED") || status.contains("위급") || status.contains("위기") || status.contains("멸종위기") || status.contains("멸종 위기")) {
            return SpeciesStatus.ENDANGERED;
        } else {
            return SpeciesStatus.GENERAL;
        }
    }

    /**
     * 학명으로 Species 조회 (존재하지 않으면 Optional.empty())
     *
     * @param scientificName 학명
     * @return Mono<Species> (빈 Mono 가능)
     */
    @Transactional(readOnly = true)
    public Mono<Species> findByScientificName(String scientificName) {
        return Mono.fromCallable(() -> speciesRepository.findByScientificName(scientificName))
            .flatMap(optional -> optional.map(Mono::just).orElseGet(Mono::empty));
    }
}
