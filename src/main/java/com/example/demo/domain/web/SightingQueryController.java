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
        summary = "기준 sighting의 geom을 중심으로 반경 내 목록",
        description = "비로그인/타인: public만, 로그인 사용자: public + 본인 private 포함",
        parameters = {
            @Parameter(name = "sightingId", in = ParameterIn.PATH, required = true,
                       description = "기준 sighting UUID"),
            @Parameter(name = "radiusMeters", in = ParameterIn.QUERY, required = false,
                       description = "반경(미터). 기본 500m",
                       schema = @Schema(type = "number", example = "500"))
        }
    )
    @GetMapping("/{sightingId}/nearby")
    public List<SightingAroundItemDto> findNearby(
            @PathVariable("sightingId") UUID sightingId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "radiusMeters", required = false) Double radiusMeters
    ) {
        UUID viewer = (principal != null) ? principal.getId() : null;
        return sightingQueryService.findAround(sightingId, viewer, radiusMeters);
    }
}
