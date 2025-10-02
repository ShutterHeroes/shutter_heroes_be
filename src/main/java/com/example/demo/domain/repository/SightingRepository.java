package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.repository.projection.SightingAroundRow;
import com.example.demo.domain.repository.projection.SightingDetailRow;
import com.example.demo.domain.repository.projection.SightingListRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sighting 엔티티의 데이터베이스 접근을 담당하는 Repository
 */
@Repository
public interface SightingRepository extends JpaRepository<Sighting, UUID> {

    @Query(value = "select exists (select 1 from app.sightings where id=:centerId)", nativeQuery = true)
    boolean existsByIdInApp(@Param("centerId") UUID centerId);

    /**
     * [기존] 기준 sighting의 geom을 중심으로 반경 :radiusMeters (m)
     * - 비로그인: public만
     * - 로그인: public + (본인 private)
     */
    @Query(value = """
        select
            s.id                                        as id,
            u.display_name                              as displayName,
            sp.common_name_ko                           as commonNameKo,
            sp.common_name_en                           as commonNameEn,
            sp.scientific_name                          as scientificName,
            sp.status::text                             as status,
            m.storage_path                              as storagePath,
            s.title                                     as title,
            s.description                               as description,
            s.occurred_at                               as occurredAt,
            s.detected_by::text                         as detectedBy,
            s.ai_confidence                             as aiConfidence,
            s.visibility::text                          as visibility,
            s.is_verified                               as isVerified,
            public.ST_AsGeoJSON(s.geom)                 as geom,
            s.created_at                                as createdAt,
            s.updated_at                                as updatedAt
        from app.sightings s
        join app.sightings c on c.id = :centerId
        join app.users     u on u.id = s.user_id
        left join app.species sp on sp.id = s.species_id
        left join app.media   m on m.id = s.media_id
        where
            public.ST_DWithin(
                public.ST_Transform(s.geom, 3857),
                public.ST_Transform(c.geom, 3857),
                CAST(:radiusMeters AS double precision)
            )
            and (
                s.visibility = 'public'
                or (:viewerId is not null and s.visibility = 'private' and s.user_id = :viewerId)
            )
        order by s.occurred_at desc nulls last, s.created_at desc
    """, nativeQuery = true)
    List<SightingAroundRow> findAroundBySighting(@Param("centerId") UUID centerId,
                                                 @Param("viewerId") UUID viewerIdNullable,
                                                 @Param("radiusMeters") double radiusMeters);

    /**
     * [신규] 지도 중심 좌표(lon, lat)를 기준으로 반경 :radiusMeters (m)
     * - 경도/위도는 WGS84(4326)로 들어온다고 가정
     * - 비로그인: public만
     * - 로그인: public + (본인 private)
     */
    @Query(value = """
        select
            s.id                                        as id,
            u.display_name                              as displayName,
            sp.common_name_ko                           as commonNameKo,
            sp.common_name_en                           as commonNameEn,
            sp.scientific_name                          as scientificName,
            sp.status::text                             as status,
            m.storage_path                              as storagePath,
            s.title                                     as title,
            s.description                               as description,
            s.occurred_at                               as occurredAt,
            s.detected_by::text                         as detectedBy,
            s.ai_confidence                             as aiConfidence,
            s.visibility::text                          as visibility,
            s.is_verified                               as isVerified,
            public.ST_AsGeoJSON(s.geom)                 as geom,
            s.created_at                                as createdAt,
            s.updated_at                                as updatedAt
        from app.sightings s
        join app.users     u on u.id = s.user_id
        left join app.species sp on sp.id = s.species_id
        left join app.media   m on m.id = s.media_id
        where
            public.ST_DWithin(
                public.ST_Transform(s.geom, 3857),
                public.ST_Transform(
                    public.ST_SetSRID(public.ST_MakePoint(:lon, :lat), 4326),
                    3857
                ),
                CAST(:radiusMeters AS double precision)
            )
            and (
                s.visibility = 'public'
                or (:viewerId is not null and s.visibility = 'private' and s.user_id = :viewerId)
            )
        order by s.occurred_at desc nulls last, s.created_at desc
    """, nativeQuery = true)
    List<SightingAroundRow> findAroundByPoint(@Param("lon") double lon,
                                              @Param("lat") double lat,
                                              @Param("viewerId") UUID viewerIdNullable,
                                              @Param("radiusMeters") double radiusMeters);

    /**
     * Sighting 전체 목록 조회 (페이징, 검색)
     * - keyword: species.scientific_name 또는 species.common_name_ko로 ILIKE 검색
     * - 비로그인: public만
     * - 로그인: public + (본인 private)
     * - sortBy와 sortOrder는 Service에서 Pageable로 전달
     */
    @Query(value = """
        select
            s.id                                        as id,
            s.title                                     as title,
            s.description                               as description,
            s.occurred_at                               as occurredAt,
            s.detected_by::text                         as detectedBy,
            s.ai_confidence                             as aiConfidence,
            s.visibility::text                          as visibility,
            s.is_verified                               as isVerified,
            s.created_at                                as createdAt,
            s.updated_at                                as updatedAt,
            u.display_name                              as displayName,
            sp.common_name_ko                           as commonNameKo,
            sp.common_name_en                           as commonNameEn,
            sp.scientific_name                          as scientificName,
            sp.status::text                             as status,
            (m.extra_info->>'sanitizedUrl')             as sanitizedUrl,
            public.ST_AsGeoJSON(s.geom)                 as geom
        from app.sightings s
        join app.users u on u.id = s.user_id
        left join app.species sp on sp.id = s.species_id
        left join app.media m on m.id = s.media_id
        where
            (
                s.visibility = 'public'
                or (:viewerId is not null and s.visibility = 'private' and s.user_id = :viewerId)
            )
            and (
                :keyword is null
                or sp.scientific_name ilike '%' || :keyword || '%'
                or sp.common_name_ko ilike '%' || :keyword || '%'
            )
        order by
            case when :sortBy = 'occurred_at' and :sortOrder = 'asc' then s.occurred_at end asc nulls last,
            case when :sortBy = 'occurred_at' and :sortOrder = 'desc' then s.occurred_at end desc nulls last,
            case when :sortBy = 'created_at' and :sortOrder = 'asc' then s.created_at end asc,
            case when :sortBy = 'created_at' and :sortOrder = 'desc' then s.created_at end desc,
            s.created_at desc
        limit :limit offset :offset
    """, nativeQuery = true)
    List<SightingListRow> findAllWithSearch(
        @Param("viewerId") UUID viewerIdNullable,
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * Sighting 전체 개수 조회 (검색 조건 포함)
     */
    @Query(value = """
        select count(*)
        from app.sightings s
        left join app.species sp on sp.id = s.species_id
        where
            (
                s.visibility = 'public'
                or (:viewerId is not null and s.visibility = 'private' and s.user_id = :viewerId)
            )
            and (
                :keyword is null
                or sp.scientific_name ilike '%' || :keyword || '%'
                or sp.common_name_ko ilike '%' || :keyword || '%'
            )
    """, nativeQuery = true)
    long countAllWithSearch(
        @Param("viewerId") UUID viewerIdNullable,
        @Param("keyword") String keyword
    );

    /**
     * Sighting 상세 조회 (ID 기준)
     * - 비로그인: public만
     * - 로그인: public + (본인 private)
     * - EXIF 정보, AI Detection 정보 포함
     */
    @Query(value = """
        select
            s.id                                        as id,
            s.title                                     as title,
            s.description                               as description,
            s.occurred_at                               as occurredAt,
            s.detected_by::text                         as detectedBy,
            s.ai_confidence                             as aiConfidence,
            s.visibility::text                          as visibility,
            s.is_verified                               as isVerified,
            s.address_text                              as addressText,
            s.created_at                                as createdAt,
            s.updated_at                                as updatedAt,
            u.id                                        as userId,
            u.display_name                              as displayName,
            u.email                                     as userEmail,
            sp.id                                       as speciesId,
            sp.common_name_ko                           as commonNameKo,
            sp.common_name_en                           as commonNameEn,
            sp.scientific_name                          as scientificName,
            sp.status::text                             as status,
            m.id                                        as mediaId,
            (m.extra_info->>'sanitizedUrl')             as sanitizedUrl,
            m.storage_path                              as storagePath,
            m.mime_type                                 as mimeType,
            m.bytes                                     as bytes,
            m.width                                     as width,
            m.height                                    as height,
            (m.extra_info->>'cameraMake')               as cameraMake,
            (m.extra_info->>'cameraModel')              as cameraModel,
            (m.extra_info->>'capturedAt')               as capturedAt,
            (m.extra_info->>'gpsLatitude')::float       as gpsLatitude,
            (m.extra_info->>'gpsLongitude')::float      as gpsLongitude,
            public.ST_AsGeoJSON(s.geom)                 as geom
        from app.sightings s
        join app.users u on u.id = s.user_id
        left join app.species sp on sp.id = s.species_id
        left join app.media m on m.id = s.media_id
        where s.id = :sightingId
            and (
                s.visibility = 'public'
                or (:viewerId is not null and s.visibility = 'private' and s.user_id = :viewerId)
            )
    """, nativeQuery = true)
    Optional<SightingDetailRow> findDetailById(
        @Param("sightingId") UUID sightingId,
        @Param("viewerId") UUID viewerIdNullable
    );
}
