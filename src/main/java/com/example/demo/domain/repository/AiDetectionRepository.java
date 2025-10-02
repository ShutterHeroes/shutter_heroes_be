package com.example.demo.domain.repository;

import com.example.demo.domain.entity.AiDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AiDetection 엔티티의 데이터베이스 접근을 담당하는 Repository
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>Vision API 감지 결과 저장</li>
 *   <li>Media별 감지 결과 조회</li>
 * </ul>
 */
@Repository
public interface AiDetectionRepository extends JpaRepository<AiDetection, UUID> {

    /**
     * 특정 Media의 모든 AI 감지 결과 조회
     *
     * @param mediaId Media ID
     * @return AI 감지 결과 리스트
     */
    List<AiDetection> findByMediaId(UUID mediaId);

    /**
     * 특정 Media의 AI 감지 결과 개수 조회
     *
     * @param mediaId Media ID
     * @return 감지된 동물 개수
     */
    long countByMediaId(UUID mediaId);
}
