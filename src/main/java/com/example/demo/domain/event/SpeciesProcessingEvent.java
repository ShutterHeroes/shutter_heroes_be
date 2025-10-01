package com.example.demo.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Species 정보 비동기 처리를 위한 이벤트 클래스
 *
 * <p><b>이벤트 발행 시점:</b></p>
 * <ul>
 *   <li>Sighting 생성 완료 후</li>
 *   <li>Vision API로 동물 인식 완료 후</li>
 *   <li>사용자에게 응답 반환 직전</li>
 * </ul>
 *
 * <p><b>이벤트 처리:</b></p>
 * <ul>
 *   <li>SpeciesProcessingEventListener가 비동기로 처리</li>
 *   <li>OpenAI API로 학명 조회 및 Species 생성</li>
 *   <li>Sighting과 Species 연결</li>
 * </ul>
 */
@Getter
public class SpeciesProcessingEvent extends ApplicationEvent {

    /**
     * 생성된 Sighting의 ID (UUID)
     */
    private final UUID sightingId;

    /**
     * Vision API에서 인식한 동물의 일반 명칭 (예: "Bengal Cat", "Golden Retriever")
     */
    private final String animalLabel;

    /**
     * Vision API에서 반환한 학명 (예: "Felis catus", "Canis lupus familiaris")
     * <p>이미 학명이 있으면 OpenAI API 호출 없이 바로 Species 생성 가능</p>
     */
    private final String scientificName;

    /**
     * Vision API의 신뢰도 (0.0 ~ 1.0)
     */
    private final Float confidence;

    /**
     * Species 처리 이벤트 생성
     *
     * @param source 이벤트를 발행한 객체 (일반적으로 SightingService)
     * @param sightingId 생성된 Sighting의 ID (UUID)
     * @param animalLabel 동물의 일반 명칭
     * @param scientificName 동물의 학명 (Vision API에서 제공 시)
     * @param confidence Vision API의 신뢰도
     */
    public SpeciesProcessingEvent(Object source, UUID sightingId, String animalLabel, String scientificName, Float confidence) {
        super(source);
        this.sightingId = sightingId;
        this.animalLabel = animalLabel;
        this.scientificName = scientificName;
        this.confidence = confidence;
    }
}
