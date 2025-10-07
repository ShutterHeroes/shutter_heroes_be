package com.example.demo.domain.web.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sighting 수정 요청 DTO
 *
 * <p>수정 가능한 필드만 포함됩니다.</p>
 * <ul>
 *   <li>title - 제목</li>
 *   <li>description - 설명</li>
 *   <li>visibility - 공개 설정 (PUBLIC, PRIVATE)</li>
 *   <li>occurredAt - 목격 시간</li>
 *   <li>addressText - 주소 텍스트</li>
 * </ul>
 *
 * <p><b>수정 불가:</b> geom (GPS), species_id (AI 감지 결과)</p>
 */
@Getter
@NoArgsConstructor
public class SightingUpdateRequest {

    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다")
    private String description;

    private String visibility;  // "PUBLIC" or "PRIVATE"

    @PastOrPresent(message = "목격 시간은 미래일 수 없습니다")
    private LocalDateTime occurredAt;

    @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
    private String addressText;
}
