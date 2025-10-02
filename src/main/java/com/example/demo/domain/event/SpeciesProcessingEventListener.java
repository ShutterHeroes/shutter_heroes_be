package com.example.demo.domain.event;

import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.entity.Species;
import com.example.demo.domain.repository.SightingRepository;
import com.example.demo.domain.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Species 정보 비동기 처리를 위한 이벤트 리스너
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>SpeciesProcessingEvent를 비동기로 처리</li>
 *   <li>OpenAI API로 학명 기반 Species 정보 조회/생성</li>
 *   <li>Sighting과 Species 연결</li>
 *   <li>에러 핸들링 및 로깅</li>
 * </ul>
 *
 * <p><b>비동기 처리 방식:</b></p>
 * <ul>
 *   <li>@Async 어노테이션으로 별도 스레드에서 실행</li>
 *   <li>@TransactionalEventListener(AFTER_COMMIT)로 트랜잭션 커밋 후 실행</li>
 *   <li>speciesTaskExecutor 스레드풀 사용</li>
 *   <li>사용자 응답과 독립적으로 처리</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeciesProcessingEventListener {

    private final SpeciesService speciesService;
    private final SightingRepository sightingRepository;

    /**
     * Species 처리 이벤트를 비동기로 처리
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>이벤트 수신 (Vision API에서 인식한 동물 정보 포함)</li>
     *   <li>트랜잭션 커밋 후 실행 (AFTER_COMMIT)</li>
     *   <li>학명 기반으로 Species 조회/생성 (OpenAI API 호출)</li>
     *   <li>Sighting 엔티티에 Species 연결</li>
     *   <li>DB 업데이트</li>
     * </ol>
     *
     * <p><b>에러 처리:</b></p>
     * <ul>
     *   <li>Species 조회/생성 실패 시 로그만 남기고 계속 진행</li>
     *   <li>Sighting 업데이트 실패 시 로그만 남기고 계속 진행</li>
     *   <li>사용자 응답에는 영향 없음 (이미 응답 완료 상태)</li>
     * </ul>
     *
     * @param event Species 처리 이벤트
     */
    @Async("speciesTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSpeciesProcessing(SpeciesProcessingEvent event) {
        log.info("Processing Species asynchronously for Sighting ID: {} (animalLabel: {}, scientificName: {})",
            event.getSightingId(), event.getAnimalLabel(), event.getScientificName());

        try {
            // 1. 학명이 있는 경우 Species 조회/생성
            if (event.getScientificName() != null && !event.getScientificName().isEmpty()) {
                Species species = speciesService.getOrCreateSpecies(event.getScientificName())
                    .block();  // 비동기 스레드에서 동기적으로 처리

                if (species != null) {
                    // 2. Sighting 조회
                    Sighting sighting = sightingRepository.findById(event.getSightingId())
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Sighting not found: " + event.getSightingId()));

                    // 3. Sighting에 Species 연결
                    sighting.setSpecies(species);
                    sightingRepository.save(sighting);

                    log.info("Species processing completed successfully for Sighting ID: {} -> Species ID: {}",
                        event.getSightingId(), species.getId());
                } else {
                    log.warn("Failed to create Species for Sighting ID: {} (scientificName: {})",
                        event.getSightingId(), event.getScientificName());
                }
            } else {
                log.info("No scientific name provided for Sighting ID: {}. Skipping Species processing.",
                    event.getSightingId());
            }

        } catch (Exception e) {
            // 비동기 처리 실패는 사용자 응답에 영향을 주지 않음
            // 로그만 남기고 계속 진행
            log.error("Error processing Species for Sighting ID: {}. Error: {}",
                event.getSightingId(), e.getMessage(), e);
        }
    }
}
