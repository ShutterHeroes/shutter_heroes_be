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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lon/lat are required when using point mode");
        }
        if (lon < -180.0 || lon > 180.0 || lat < -90.0 || lat > 90.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lon/lat out of range");
        }
    }

    /** 통합 진입점: lon/lat가 있으면 point 기준, 없으면 centerId 기준 */
    public List<SightingAroundItemDto> findNearbyUnified(UUID centerId, Double lon, Double lat,
                                                         UUID viewerIdNullable, Double radiusMeters) {

        double r = normalizeRadius(radiusMeters);

        // 1) lon/lat 우선
        if (lon != null || lat != null) {
            validateLonLat(lon, lat);
            List<SightingAroundRow> rows = sightingRepository.findAroundByPoint(lon, lat, viewerIdNullable, r);
            return rows.stream().map(this::mapRow).toList();
        }

        // 2) centerId 사용
        if (centerId != null) {
            if (!sightingRepository.existsByIdInApp(centerId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "center sighting not found");
            }
            List<SightingAroundRow> rows =
                    sightingRepository.findAroundBySighting(centerId, viewerIdNullable, r);
            return rows.stream().map(this::mapRow).toList();
        }

        // 3) 아무것도 없으면 400
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "centerId or lon/lat must be provided");
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
