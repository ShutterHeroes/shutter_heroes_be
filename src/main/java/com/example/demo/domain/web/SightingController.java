package com.example.demo.domain.web;

import com.example.demo.domain.dto.response.SightingCreateResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.domain.service.SightingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@RequestMapping("/api/sightings")
@RequiredArgsConstructor
@Tag(name = "Sighting", description = "동물 목격 정보 API")
public class SightingController {

    private final SightingService sightingService;
    private final UserRepository userRepository;

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
}
