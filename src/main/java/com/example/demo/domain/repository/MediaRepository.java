package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Media;
import com.example.demo.domain.repository.projection.MediaBrowseRow;
import com.example.demo.domain.web.dto.MediaBrowseItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    /**
     * [내 업로드] sighting 여부와 무관하게,
     * 내가 올린 Media를 최신순으로 반환합니다.
     * JPQL constructor expression을 사용하여 DTO로 바로 매핑합니다.
     *
     * 주의: Media 엔티티의 업로더 연관명이 m.user 라고 가정합니다.
     * (필요시 m.owner 등 실제 필드명으로 바꾸세요)
     */
    @Query("""
        select new com.example.demo.domain.web.dto.MediaBrowseItemDto(
            m.id, m.storagePath, m.mimeType, m.width, m.height, m.createdAt,
            m.user.id, null, null
        )
        from Media m
        where m.user.id = :ownerId
        order by m.createdAt desc
    """)
    Page<MediaBrowseItemDto> pageMyUploads(@Param("ownerId") UUID ownerId, Pageable pageable);

    /**
     * [브라우즈] sighting과 조인하여 가시성 규칙 적용
     *  - public  : 모두 볼 수 있음
     *  - private : 작성자(=s.user_id)만 볼 수 있음
     *
     * DB enum(app.visibility)이 'public'/'private' 소문자 값이므로
     * 네이티브 SQL에서 소문자 리터럴로 비교합니다.
     * 결과는 인터페이스 프로젝션(MediaBrowseRow)로 받습니다.
     */
    @Query(
        value = """
            select distinct
                m.id           as mediaId,
                m.storage_path as storagePath,
                m.mime_type    as mimeType,
                m.width        as width,
                m.height       as height,
                m.created_at   as createdAt,
                s.user_id      as ownerId,
                s.id           as sightingId,
                s.visibility   as sightingVisibility
            from app.media m
            join app.sightings s on s.media_id = m.id
            where
                s.visibility = 'public'
                or (
                    :viewerId is not null
                    and s.visibility = 'private'
                    and s.user_id = :viewerId
                )
            order by m.created_at desc
        """,
        countQuery = """
            select count(distinct m.id)
            from app.media m
            join app.sightings s on s.media_id = m.id
            where
                s.visibility = 'public'
                or (
                    :viewerId is not null
                    and s.visibility = 'private'
                    and s.user_id = :viewerId
                )
        """,
        nativeQuery = true
    )
    Page<MediaBrowseRow> pageVisibleFor(@Param("viewerId") UUID viewerId, Pageable pageable);
}
