package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.repository.projection.SightingAroundRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SightingRepository extends JpaRepository<Sighting, UUID> {

    @Query(value = "select exists (select 1 from app.sightings where id=:centerId)", nativeQuery = true)
    boolean existsByIdInApp(@Param("centerId") UUID centerId);

    /**
     * 기준 sighting의 geom을 중심으로 반경 :radiusMeters (m) 내 결과 반환(비페이지네이션)
     * - 비로그인: public만
     * - 로그인: public + (본인 private)
     * - PostGIS 함수는 public. 접두사 사용 (search_path=app 환경 대응)
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
                CAST(:radiusMeters AS double precision)   -- 동적 반경(m)
            )
            and (
                s.visibility = 'public'
                or (
                    :viewerId is not null
                    and s.visibility = 'private'
                    and s.user_id = :viewerId
                )
            )
        order by s.occurred_at desc nulls last, s.created_at desc
    """, nativeQuery = true)
    List<SightingAroundRow> findAroundBySighting(@Param("centerId") UUID centerId,
                                                 @Param("viewerId") UUID viewerIdNullable,
                                                 @Param("radiusMeters") double radiusMeters);
}
