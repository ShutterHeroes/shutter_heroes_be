package com.example.demo.domain.web;

import com.example.demo.config.security.oauth2.UserPrincipal;
import com.example.demo.domain.service.MediaManageService;
import com.example.demo.domain.web.dto.DeleteMediaResponse;
import com.example.demo.domain.web.dto.UpdateVisibilityRequest;
import com.example.demo.domain.web.dto.UpdateVisibilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media: Manage")
public class MediaManageController {

    private final MediaManageService mediaManageService;

    private boolean isAdmin(UserPrincipal principal) {
        return principal != null && principal.getAuthorities() != null &&
               principal.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }

    @Operation(
        summary = "미디어의 sighting 가시성 변경 (public/private)",
        description = "본인 또는 관리자만 가능. visibility는 public 혹은 private(대소문자 무시).",
        parameters = {
            @Parameter(
                name = "mediaId",
                in = ParameterIn.PATH,
                required = true,
                description = "대상 Media의 UUID",
                schema = @Schema(type = "string", format = "uuid")
            )
        }
    )
    @PatchMapping(value = "/{mediaId}/visibility", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public UpdateVisibilityResponse updateVisibility(
            @PathVariable("mediaId") UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateVisibilityRequest.class),
                    examples = {
                        @ExampleObject(name = "public",  value = "{\"visibility\":\"public\"}"),
                        @ExampleObject(name = "private", value = "{\"visibility\":\"private\"}")
                    }
                )
            )
            @org.springframework.web.bind.annotation.RequestBody UpdateVisibilityRequest request
    ) {
        return mediaManageService.changeVisibility(mediaId, principal.getId(), isAdmin(principal), request.getVisibility());
    }

    @Operation(
        summary = "미디어 삭제",
        description = "본인 또는 관리자만 가능. 먼저 sighting을 삭제한 뒤 media를 삭제합니다.",
        parameters = {
            @Parameter(
                name = "mediaId",
                in = ParameterIn.PATH,
                required = true,
                description = "삭제할 Media의 UUID",
                schema = @Schema(type = "string", format = "uuid")
            )
        }
    )
    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.OK)
    public DeleteMediaResponse deleteMedia(
        @PathVariable("mediaId") UUID mediaId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {        
        return mediaManageService.deleteMedia(mediaId, principal.getId(), isAdmin(principal));
    }
}
