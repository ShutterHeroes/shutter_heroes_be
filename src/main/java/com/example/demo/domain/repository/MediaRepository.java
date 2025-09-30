package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Media;
import com.example.demo.domain.repository.projection.MediaBrowseRow;
import com.example.demo.domain.web.dto.MediaBrowseItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

     /** 소유자 확인: media의 user_id를 조회 (없으면 null) */
    @Query(value = "select user_id from app.media where id = :mediaId", nativeQuery = true)
    UUID findOwnerIdByMediaId(@Param("mediaId") UUID mediaId);

    /** [USER 전용] 내 미디어에 연결된 내 sighting들의 visibility 일괄 변경 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        update app.sightings
           set visibility = CAST(:visibility AS app.visibility)
         where media_id = :mediaId
           and user_id  = :ownerId
    """, nativeQuery = true)
    int updateSightingVisibilityForOwner(@Param("mediaId") UUID mediaId,
                                         @Param("ownerId") UUID ownerId,
                                         @Param("visibility") String visibilityLowercase);

    /** [ADMIN 전용] media에 연결된 모든 sighting들의 visibility 일괄 변경 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        update app.sightings
           set visibility = CAST(:visibility AS app.visibility)
         where media_id = :mediaId
    """, nativeQuery = true)
    int updateSightingVisibilityAsAdmin(@Param("mediaId") UUID mediaId,
                                        @Param("visibility") String visibilityLowercase);

    /** [USER 전용] 내 미디어의 sighting 먼저 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        delete from app.sightings
         where media_id = :mediaId
           and user_id  = :ownerId
    """, nativeQuery = true)
    int deleteSightingsByMediaForOwner(@Param("mediaId") UUID mediaId,
                                       @Param("ownerId") UUID ownerId);

    /** [ADMIN 전용] 미디어의 모든 sighting 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "delete from app.sightings where media_id = :mediaId", nativeQuery = true)
    int deleteSightingsByMediaAsAdmin(@Param("mediaId") UUID mediaId);

    /** [USER 전용] 본인 소유 media 삭제 (외래키 오류 예방 위해 먼저 sighting 삭제 후 호출) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "delete from app.media where id = :mediaId and user_id = :ownerId", nativeQuery = true)
    int deleteMediaByIdForOwner(@Param("mediaId") UUID mediaId,
                                @Param("ownerId") UUID ownerId);

    /** [ADMIN 전용] media 강제 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "delete from app.media where id = :mediaId", nativeQuery = true)
    int deleteMediaByIdAsAdmin(@Param("mediaId") UUID mediaId);
}
