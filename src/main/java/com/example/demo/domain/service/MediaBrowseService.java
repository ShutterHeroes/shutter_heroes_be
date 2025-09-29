package com.example.demo.domain.service;

import com.example.demo.domain.web.dto.MediaBrowseItemDto;
import com.example.demo.domain.repository.MediaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaBrowseService {

    private final MediaRepository mediaRepository;

    /** 내 업로드(로그인 필요) */
    public Page<MediaBrowseItemDto> pageMyUploads(UUID userId, Pageable pageable) {
        return mediaRepository.pageMyUploads(userId, pageable);
    }

    /** 가시성 규칙을 적용한 브라우징(비로그인=PUBLIC만, 로그인=PUBLIC + 내 PRIVATE) */
    public Page<MediaBrowseItemDto> pageVisible(UUID viewerId, Pageable pageable) {
        Page<com.example.demo.domain.repository.projection.MediaBrowseRow> page =
                mediaRepository.pageVisibleFor(viewerId, pageable);

        return page.map(r -> new MediaBrowseItemDto(
                r.getMediaId(),
                r.getStoragePath(),
                r.getMimeType(),
                r.getWidth(),
                r.getHeight(),
                // LocalDateTime -> OffsetDateTime (서버 시스템 타임존 기준)
                r.getCreatedAt() != null ? r.getCreatedAt().atOffset(java.time.OffsetDateTime.now().getOffset()) : null,
                r.getOwnerId(),
                r.getSightingId(),
                // 'public'/'private' -> Enum 매핑 (대소문자 무시)
                "public".equalsIgnoreCase(r.getSightingVisibility())
                        ? com.example.demo.domain.enums.Visibility.PUBLIC
                        : com.example.demo.domain.enums.Visibility.PRIVATE
        ));
    }

}
