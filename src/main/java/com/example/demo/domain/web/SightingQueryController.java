package com.example.demo.domain.web;

import com.example.demo.config.security.oauth2.UserPrincipal;
import com.example.demo.domain.service.SightingQueryService;
import com.example.demo.domain.web.dto.SightingAroundItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sightings")
@Tag(name = "Sightings: Query")
public class SightingQueryController {

    private final SightingQueryService sightingQueryService;

    @Operation(
        summary = "반경 내 sighting 조회(통합)",
        description = "lon/lat가 있으면 좌표 기준, 없으면 centerId 기준으로 500m(기본) 내 조회. " +
                      "비로그인/타인: public만, 로그인: public + 본인 private 포함.",
        parameters = {
            @Parameter(name = "centerId", in = ParameterIn.QUERY, required = false,
                       description = "기준 sighting UUID (lon/lat 없을 때만 사용)",
                       schema = @Schema(type = "string", format = "uuid")),
            @Parameter(name = "lon", in = ParameterIn.QUERY, required = false,
                       description = "경도(-180~180). 존재 시 좌표 기준을 우선 사용",
                       schema = @Schema(type = "number", example = "126.9784")),
            @Parameter(name = "lat", in = ParameterIn.QUERY, required = false,
                       description = "위도(-90~90). 존재 시 좌표 기준을 우선 사용",
                       schema = @Schema(type = "number", example = "37.5667")),
            @Parameter(name = "radiusMeters", in = ParameterIn.QUERY, required = false,
                       description = "반경(미터). 기본 500m",
                       schema = @Schema(type = "number", example = "500"))
        }
    )
    @GetMapping("/nearby")
    public List<SightingAroundItemDto> findNearbyUnified(
            @RequestParam(name = "centerId", required = false) UUID centerId,
            @RequestParam(name = "lon", required = false) Double lon,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "radiusMeters", required = false) Double radiusMeters,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID viewer = (principal != null) ? principal.getId() : null;
        return sightingQueryService.findNearbyUnified(centerId, lon, lat, viewer, radiusMeters);
    }

    /** (선택) 이전 엔드포인트 유지 시, 통합 로직을 그대로 위임. 필요 없으면 삭제해도 됩니다. */
    @Deprecated
    @GetMapping("/{sightingId}/nearby")
    public List<SightingAroundItemDto> findNearbyBySightingDeprecated(
            @PathVariable("sightingId") UUID sightingId,
            @RequestParam(name = "radiusMeters", required = false) Double radiusMeters,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID viewer = (principal != null) ? principal.getId() : null;
        return sightingQueryService.findNearbyUnified(sightingId, null, null, viewer, radiusMeters);
    }
}
