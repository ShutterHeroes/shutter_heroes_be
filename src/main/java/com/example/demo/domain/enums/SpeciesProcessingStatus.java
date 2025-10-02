package com.example.demo.domain.enums;

/**
 * Species 처리 상태를 나타내는 Enum
 *
 * <p><b>상태 종류:</b></p>
 * <ul>
 *   <li>PENDING: 백그라운드에서 Species 처리 중</li>
 *   <li>COMPLETED: Species 처리 완료 (이미 DB에 존재하는 경우)</li>
 *   <li>NOT_DETECTED: 동물이 인식되지 않음</li>
 * </ul>
 *
 * <p><b>사용 목적:</b></p>
 * <ul>
 *   <li>사용자에게 Species 처리 상태를 알려줌</li>
 *   <li>프론트엔드에서 폴링 또는 WebSocket 연결 여부 결정</li>
 * </ul>
 */
public enum SpeciesProcessingStatus {
    /**
     * 백그라운드에서 Species 처리 중
     * <p>OpenAI API를 호출하여 Species 정보를 가져오는 중</p>
     */
    PENDING,

    /**
     * Species 처리 완료
     * <p>이미 DB에 존재하는 Species이거나 처리가 완료됨</p>
     */
    COMPLETED,

    /**
     * 동물이 인식되지 않음
     * <p>Vision API에서 동물을 인식하지 못함</p>
     */
    NOT_DETECTED
}
