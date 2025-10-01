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
    private static final double MIN_RADIUS_M = 10.0;     // 너무 작으면 0 근처 오차 방지
    private static final double MAX_RADIUS_M = 20000.0;  // 20km 상한 (필요 시 조정)

    private final SightingRepository sightingRepository;

    public List<SightingAroundItemDto> findAround(UUID centerSightingId, UUID viewerIdNullable, Double radiusMeters) {
        if (!sightingRepository.existsByIdInApp(centerSightingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "center sighting not found");
        }

        double r = (radiusMeters == null) ? DEFAULT_RADIUS_M : radiusMeters.doubleValue();
        if (Double.isNaN(r) || Double.isInfinite(r)) r = DEFAULT_RADIUS_M;
        if (r < MIN_RADIUS_M) r = MIN_RADIUS_M;
        if (r > MAX_RADIUS_M) r = MAX_RADIUS_M;

        List<SightingAroundRow> rows =
                sightingRepository.findAroundBySighting(centerSightingId, viewerIdNullable, r);

        return rows.stream()
                .map(rw -> new SightingAroundItemDto(
                        rw.getId(),
                        rw.getDisplayName(),
                        rw.getCommonNameKo(),
                        rw.getCommonNameEn(),
                        rw.getScientificName(),
                        rw.getStatus(),
                        rw.getStoragePath(),
                        rw.getTitle(),
                        rw.getDescription(),
                        rw.getOccurredAt(),
                        rw.getDetectedBy(),
                        rw.getAiConfidence(),
                        rw.getVisibility(),
                        rw.getIsVerified(),
                        rw.getGeom(),
                        rw.getCreatedAt(),
                        rw.getUpdatedAt()
                ))
                .toList();
    }
}
