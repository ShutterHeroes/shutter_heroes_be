package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.repository.projection.SightingAroundRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Sighting 엔티티의 데이터베이스 접근을 담당하는 Repository
 */
@Repository
public interface SightingRepository extends JpaRepository<Sighting, UUID> {

    @Query(value = "select exists (select 1 from app.sightings where id=:centerId)", nativeQuery = true)
    boolean existsByIdInApp(@Param("centerId") UUID centerId);

    /**
     * 기준 sighting의 geom을 중심으로 반경 500m 내 결과 반환(비페이지네이션)
     * - 비로그인: public만
     * - 로그인: public + (본인 private)
     * - geom 문자열: s.geom::text (WKT)
     * - 거리 계산: geometry -> EPSG:3857 로 변환하여 미터 단위 500m
     * - search_path 가 app 뿐이어도 동작하도록 PostGIS 함수는 public. 접두사로 호출
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
            s.geom::text                                as geom,
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
                500
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
                                                 @Param("viewerId") UUID viewerIdNullable);
}
