package com.example.demo.domain.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Sighting 목록 조회 응답 DTO (페이징 래퍼)
 *
 * <p>페이징 메타데이터와 실제 데이터를 함께 반환합니다.</p>
 */
@Getter
@AllArgsConstructor
public class SightingListResponse {
    private final List<SightingListItemDto> content;  // 실제 데이터
    private final long totalElements;                 // 전체 데이터 개수
    private final int totalPages;                     // 전체 페이지 수
    private final int currentPage;                    // 현재 페이지 번호 (0부터 시작)
    private final int size;                           // 페이지 크기
    private final boolean first;                      // 첫 페이지 여부
    private final boolean last;                       // 마지막 페이지 여부

    /**
     * 페이징 정보와 함께 응답 생성
     */
    public static SightingListResponse of(
        List<SightingListItemDto> content,
        long totalElements,
        int currentPage,
        int size
    ) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = currentPage == 0;
        boolean last = currentPage >= totalPages - 1 || totalPages == 0;

        return new SightingListResponse(
            content,
            totalElements,
            totalPages,
            currentPage,
            size,
            first,
            last
        );
    }
}
