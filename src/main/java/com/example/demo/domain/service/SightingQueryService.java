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

    private final SightingRepository sightingRepository;

    public List<SightingAroundItemDto> findAround(UUID centerSightingId, UUID viewerIdNullable) {
        // 기준 sighting 없으면 404
        if (!sightingRepository.existsByIdInApp(centerSightingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "center sighting not found");
        }

        List<SightingAroundRow> rows =
                sightingRepository.findAroundBySighting(centerSightingId, viewerIdNullable);

        return rows.stream()
                .map(r -> new SightingAroundItemDto(
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
                ))
                .toList();
    }
}
