package com.example.demo.domain.service;

import com.example.demo.domain.repository.SightingRepository;
import com.example.demo.domain.repository.projection.SightingAroundRow;
import com.example.demo.domain.web.dto.SightingAroundItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SightingQueryService {

    private static final double DEFAULT_RADIUS_M = 500.0;
    private static final double MIN_RADIUS_M = 10.0;
    private static final double MAX_RADIUS_M = 20000.0;

    private final SightingRepository sightingRepository;

    private double normalizeRadius(Double r) {
        double v = (r == null) ? DEFAULT_RADIUS_M : r;
        if (Double.isNaN(v) || Double.isInfinite(v)) v = DEFAULT_RADIUS_M;
        if (v < MIN_RADIUS_M) v = MIN_RADIUS_M;
        if (v > MAX_RADIUS_M) v = MAX_RADIUS_M;
        return v;
    }

    private void validateLonLat(Double lon, Double lat) {
        if (lon == null || lat == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lon/lat are required");
        }
        if (lon < -180.0 || lon > 180.0 || lat < -90.0 || lat > 90.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lon/lat out of range");
        }
    }

    /** 기준 sightingId 기준 검색 */
    public List<SightingAroundItemDto> findAround(UUID centerSightingId, UUID viewerIdNullable, Double radiusMeters) {
        if (!sightingRepository.existsByIdInApp(centerSightingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "center sighting not found");
        }
        double r = normalizeRadius(radiusMeters);

        List<SightingAroundRow> rows =
                sightingRepository.findAroundBySighting(centerSightingId, viewerIdNullable, r);

        return rows.stream().map(this::mapRow).toList();
    }

    /** 지도 중심 좌표 기준 검색 */
    public List<SightingAroundItemDto> findAroundByPoint(Double lon, Double lat, UUID viewerIdNullable, Double radiusMeters) {
        validateLonLat(lon, lat);
        double r = normalizeRadius(radiusMeters);

        List<SightingAroundRow> rows =
                sightingRepository.findAroundByPoint(lon, lat, viewerIdNullable, r);

        return rows.stream().map(this::mapRow).toList();
    }

    private SightingAroundItemDto mapRow(SightingAroundRow r) {
        return new SightingAroundItemDto(
                r.getId(),
                r.getDisplayName(),
                r.getCommonNameKo(),
                r.getCommonNameEn(),
                r.getScientificName(),
                r.getStatus(),
                r.getStoragePath(),
                r.getTitle(),
                r.getDescription(),
                r.getOccurredAt(),
                r.getDetectedBy(),
                r.getAiConfidence(),
                r.getVisibility(),
                r.getIsVerified(),
                r.getGeom(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
