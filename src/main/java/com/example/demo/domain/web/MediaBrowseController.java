package com.example.demo.domain.web;

// package 등 기존 동일
import com.example.demo.config.security.oauth2.UserPrincipal;
import com.example.demo.domain.service.MediaBrowseService;
import com.example.demo.domain.web.dto.MediaBrowseItemDto;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/medias")
public class MediaBrowseController {

    private final MediaBrowseService mediaBrowseService;

    /**
     * [내 업로드 목록]
     * - 인증 필요
     * - sighting과 무관하게 내가 올린 모든 Media를 storage_path 기준으로 반환
     */
    @GetMapping("/my")
    public Page<MediaBrowseItemDto> pageMyUploads(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        UUID me = principal.getId();
        return mediaBrowseService.pageMyUploads(me, pageable);
    }

    /**
     * [브라우즈(피드)]
     * - 비로그인: sighting.visibility=PUBLIC 인 것만 노출
     * - 로그인: PUBLIC + (내가 작성한 PRIVATE) 노출
     * - 예약어 충돌 방지를 위해 쿼리 파라미터에 'public'/'private' 같은 단어를 사용하지 않음
     */
    @GetMapping("/browse")
    public Page<MediaBrowseItemDto> browse(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        UUID viewerId = (principal != null) ? principal.getId() : null;
        return mediaBrowseService.pageVisible(viewerId, pageable);
    }
}
