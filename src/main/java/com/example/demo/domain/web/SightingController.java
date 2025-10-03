package com.example.demo.domain.web;

import com.example.demo.config.security.oauth2.UserPrincipal;
import com.example.demo.domain.dto.response.SightingCreateResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.domain.service.SightingService;
import com.example.demo.domain.web.dto.SightingDeleteResponse;
import com.example.demo.domain.web.dto.SightingDetailResponse;
import com.example.demo.domain.web.dto.SightingListResponse;
import com.example.demo.domain.web.dto.SightingUpdateRequest;
import com.example.demo.domain.web.dto.SightingUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Sighting 관련 API를 제공하는 Controller
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>이미지 업로드 및 동물 인식</li>
 *   <li>Sighting 생성 (비동기 Species 처리)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sightings")
@RequiredArgsConstructor
@Tag(name = "Sighting", description = "동물 목격 정보 API")
public class SightingController {

    private final SightingService sightingService;
    private final UserRepository userRepository;

    /**
     * Sighting 전체 목록 조회 (페이징, 검색)
     */
    @Operation(
        summary = "Sighting 전체 목록 조회 (페이징, 검색)",
        description = "페이징 처리된 Sighting 목록을 조회합니다. " +
                      "keyword로 학명(scientific_name) 또는 한국어 이름(common_name_ko), 또는 영어 이름(common_name_en)을 검색할 수 있습니다. " +
                      "비로그인: public만 조회, 로그인: public + 본인 private 조회 가능. " +
                      "기본 정렬: createdAt DESC, 기본 페이지 크기: 20"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 파라미터)"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public SightingListResponse getAllSightings(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(name = "keyword", required = false) String keyword,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID viewer = (principal != null) ? principal.getId() : null;
        return sightingService.findAllSightings(viewer, keyword, pageable);
    }

    /**
     * 내가 제보한 Sighting 목록 조회 (페이징, 검색)
     */
    @Operation(
        summary = "내가 제보한 Sighting 목록 조회 (페이징, 검색)",
        description = "로그인한 사용자가 제보한 Sighting 목록을 조회합니다. " +
                      "keyword로 학명(scientific_name) 또는 한국어 이름(common_name_ko), 또는 영어 이름(common_name_en)을 검색할 수 있습니다. " +
                      "본인이 작성한 모든 Sighting 조회 (public/private 모두 포함). " +
                      "기본 정렬: createdAt DESC, 기본 페이지 크기: 20"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 파라미터)"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my")
    public SightingListResponse getMySightings(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(name = "keyword", required = false) String keyword,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return sightingService.findMyReports(principal.getId(), keyword, pageable);
    }

    /**
     * Sighting 상세 조회
     */
    @Operation(
        summary = "Sighting 상세 조회",
        description = "Sighting ID로 상세 정보를 조회합니다. " +
                      "Public: 모든 사용자 조회 가능, Private: 소유자 또는 ADMIN만 조회 가능. " +
                      "소유자인 경우 원본 이미지 URL(EXIF 포함)도 함께 반환됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
        @ApiResponse(responseCode = "404", description = "Sighting을 찾을 수 없거나 권한이 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{sightingId}")
    public SightingDetailResponse getSightingDetail(
        @PathVariable UUID sightingId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID viewer = (principal != null) ? principal.getId() : null;
        return sightingService.findSightingDetail(sightingId, viewer);
    }

    /**
     * 이미지를 업로드하고 Sighting을 생성합니다.
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>이미지 파일 검증</li>
     *   <li>Vision API로 동물 인식 (최대 5개)</li>
     *   <li>Sighting 생성 및 저장</li>
     *   <li>사용자에게 즉시 응답 (3-7초)</li>
     *   <li>백그라운드에서 Species 처리</li>
     * </ol>
     *
     * <p><b>응답 정보:</b></p>
     * <ul>
     *   <li>sightingId: 생성된 Sighting ID</li>
     *   <li>detections: Vision API 인식 결과 (최대 5개)</li>
     *   <li>speciesProcessingStatus: PENDING (Species 처리 중) 또는 NOT_DETECTED (동물 미인식)</li>
     * </ul>
     *
     * @param imageFile 업로드할 이미지 파일 (JPEG, PNG 등)
     * @param title Sighting 제목 (선택)
     * @param description Sighting 설명 (선택)
     * @param userDetails 현재 로그인한 사용자 정보 (Spring Security)
     * @return 생성된 Sighting 정보 및 Vision API 결과
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Sighting 생성",
        description = "이미지를 업로드하고 동물을 인식하여 Sighting을 생성합니다. " +
            "Vision API 결과는 즉시 반환되며, Species 정보는 백그라운드에서 처리됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sighting 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미지 파일 형식 오류 등)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<SightingCreateResponse> createSighting(
        @Parameter(description = "업로드할 이미지 파일", required = true)
        @RequestParam("image") MultipartFile imageFile,

        @Parameter(description = "Sighting 제목")
        @RequestParam(value = "title", required = false) String title,

        @Parameter(description = "Sighting 설명")
        @RequestParam(value = "description", required = false) String description,

        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Creating Sighting for user: {} with image: {}", userDetails.getUsername(), imageFile.getOriginalFilename());

        // 사용자 조회
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

        // Sighting 생성 (비동기 Species 처리)
        SightingService.SightingCreateResult result = sightingService.createSighting(
            imageFile,
            user,
            title,
            description
        );

        // 응답 DTO 생성
        SightingCreateResponse response = SightingCreateResponse.of(
            result.getSighting(),
            result.getMedia(),
            result.getDetections(),
            result.isSpeciesProcessing()
        );

        log.info("Sighting created successfully: {} (speciesProcessingStatus: {})",
            response.getSightingId(), response.getSpeciesProcessingStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Sighting 수정
     */
    @Operation(
        summary = "Sighting 수정",
        description = "Sighting 정보를 수정합니다. " +
                      "수정 가능한 필드: title, description, visibility, occurredAt, addressText. " +
                      "소유자 또는 ADMIN만 수정 가능합니다. " +
                      "GPS(geom)와 AI 감지 결과(species_id)는 수정할 수 없습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 파라미터)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (소유자 또는 ADMIN만 수정 가능)"),
        @ApiResponse(responseCode = "404", description = "Sighting을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{sightingId}")
    public SightingUpdateResponse updateSighting(
        @PathVariable UUID sightingId,
        @Valid @RequestBody SightingUpdateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = isAdmin(principal);
        return sightingService.updateSighting(sightingId, principal.getId(), isAdmin, request);
    }

    /**
     * Sighting 삭제
     */
    @Operation(
        summary = "Sighting 삭제",
        description = "Sighting을 삭제합니다. " +
            "소유자 또는 ADMIN만 삭제 가능합니다. " +
            "Hard Delete 방식으로 데이터베이스에서 완전히 삭제됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (소유자 또는 ADMIN만 삭제 가능)"),
        @ApiResponse(responseCode = "404", description = "Sighting을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{sightingId}")
    public SightingDeleteResponse deleteSighting(
        @PathVariable UUID sightingId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = isAdmin(principal);
        return sightingService.deleteSighting(sightingId, principal.getId(), isAdmin);
    }

    /**
     * 현재 사용자가 ADMIN 권한을 가지고 있는지 확인합니다.
     */
    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
