package com.example.demo.domain.service;

import com.example.demo.domain.dto.exif.ExifMetadata;
import com.example.demo.domain.dto.vision.AnimalDetection;
import com.example.demo.domain.entity.AiDetection;
import com.example.demo.domain.entity.Media;
import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.DetectedBy;
import com.example.demo.domain.enums.Visibility;
import com.example.demo.domain.event.SpeciesProcessingEvent;
import com.example.demo.domain.repository.AiDetectionRepository;
import com.example.demo.domain.repository.MediaRepository;
import com.example.demo.domain.repository.SightingRepository;
import com.example.demo.domain.repository.projection.SightingDetailRow;
import com.example.demo.domain.repository.projection.SightingListRow;
import com.example.demo.domain.web.dto.SightingDeleteResponse;
import com.example.demo.domain.web.dto.SightingDetailResponse;
import com.example.demo.domain.web.dto.SightingListItemDto;
import com.example.demo.domain.web.dto.SightingListResponse;
import com.example.demo.domain.web.dto.SightingUpdateRequest;
import com.example.demo.domain.web.dto.SightingUpdateResponse;
import com.example.demo.exceptions.errorcode.SightingErrorCode;
import com.example.demo.exceptions.exception.SightingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Sighting 생성 및 관리를 담당하는 서비스
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>S3 이미지 업로드</li>
 *   <li>EXIF 메타데이터 추출</li>
 *   <li>Vision API 동물 인식</li>
 *   <li>Sighting 및 Media 엔티티 생성</li>
 *   <li>Species 정보 비동기 처리 이벤트 발행</li>
 * </ul>
 *
 * <p><b>처리 흐름:</b></p>
 * <ol>
 *   <li>S3에 이미지 업로드</li>
 *   <li>EXIF 메타데이터 추출 (GPS, 촬영시간, 카메라정보)</li>
 *   <li>Vision API로 동물 인식</li>
 *   <li>Media 엔티티 생성 및 저장</li>
 *   <li>Sighting 엔티티 생성 및 저장 (EXIF GPS, 촬영시간 반영)</li>
 *   <li>사용자에게 즉시 응답 반환 (3-7초)</li>
 *   <li>Species 처리 이벤트 발행 (백그라운드)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository sightingRepository;
    private final MediaRepository mediaRepository;
    private final AiDetectionRepository aiDetectionRepository;
    private final AnimalVisionService animalVisionService;
    private final YoloInferenceService yoloInferenceService;
    private final YoloCallbackService yoloCallbackService;
    private final S3Service s3Service;
    private final ExifService exifService;
    private final ExifRemovalService exifRemovalService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${yolo.confidence.threshold:0.9}")
    private float yoloConfidenceThreshold;

    // ========== Public Methods (Controller 순서와 동일) ==========

    /**
     * Sighting 전체 목록 조회 (페이징, 검색)
     *
     * @param viewerIdNullable 로그인 사용자 ID (비로그인 시 null)
     * @param keyword 검색어 (학명 또는 한국어 이름, 영어 이름. null이면 전체 조회)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징 처리된 Sighting 목록
     */
    @Transactional(readOnly = true)
    public SightingListResponse findAllSightings(
        UUID viewerIdNullable,
        String keyword,
        Pageable pageable
    ) {
        // 검색어 정규화 (빈 문자열을 null로 변환)
        String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

        // Pageable에서 정렬 정보 추출
        String sortBy = "created_at";  // 기본값
        String sortOrder = "desc";     // 기본값

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            sortBy = convertPropertyToColumn(order.getProperty());
            sortOrder = order.isAscending() ? "asc" : "desc";
        }

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        // 데이터 조회
        List<SightingListRow> rows = sightingRepository.findAllWithSearch(
            viewerIdNullable,
            normalizedKeyword,
            sortBy,
            sortOrder,
            size,
            offset
        );

        // 전체 개수 조회
        long totalElements = sightingRepository.countAllWithSearch(viewerIdNullable, normalizedKeyword);

        // DTO 변환
        List<SightingListItemDto> items = rows.stream()
            .map(this::mapListRow)
            .toList();

        return SightingListResponse.of(items, totalElements, page, size);
    }

    /**
     * Sighting 상세 조회
     *
     * @param sightingId Sighting ID
     * @param viewerIdNullable 로그인 사용자 ID (비로그인 시 null)
     * @return Sighting 상세 정보
     */
    @Transactional(readOnly = true)
    public SightingDetailResponse findSightingDetail(UUID sightingId, UUID viewerIdNullable) {
        SightingDetailRow row = sightingRepository.findDetailById(sightingId, viewerIdNullable)
            .orElseThrow(() -> new SightingException(SightingErrorCode.NOT_FOUND));

        return mapDetailRow(row, viewerIdNullable);
    }

    /**
     * 내가 제보한 Sighting 목록 조회 (페이징, 검색)
     *
     * @param userId 조회할 사용자 ID
     * @param keyword 검색어 (학명 또는 한국어 이름, 영어 이름. null이면 전체 조회)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징 처리된 Sighting 목록
     */
    @Transactional(readOnly = true)
    public SightingListResponse findMyReports(
        UUID userId,
        String keyword,
        Pageable pageable
    ) {
        // 검색어 정규화 (빈 문자열을 null로 변환)
        String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

        // Pageable에서 정렬 정보 추출
        String sortBy = "created_at";  // 기본값
        String sortOrder = "desc";     // 기본값

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            sortBy = convertPropertyToColumn(order.getProperty());
            sortOrder = order.isAscending() ? "asc" : "desc";
        }

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        // 데이터 조회
        List<SightingListRow> rows = sightingRepository.findMyReports(
            userId,
            normalizedKeyword,
            sortBy,
            sortOrder,
            size,
            offset
        );

        // 전체 개수 조회
        long totalElements = sightingRepository.countMyReports(userId, normalizedKeyword);

        // DTO 변환
        List<SightingListItemDto> items = rows.stream()
            .map(this::mapListRow)
            .toList();

        return SightingListResponse.of(items, totalElements, page, size);
    }

    /**
     * 이미지를 업로드하고 Sighting을 생성합니다.
     *
     * <p><b>동작 방식:</b></p>
     * <ol>
     *   <li>EXIF 메타데이터 추출 (GPS, 촬영시간, 카메라정보)</li>
     *   <li>EXIF 제거 이미지 생성</li>
     *   <li>S3에 두 버전 업로드 (원본 + EXIF 제거)</li>
     *   <li>Vision API로 동물 인식 (동물 없으면 예외 발생)</li>
     *   <li>FastAPI YOLO 추론 요청 (비동기)</li>
     *   <li>Media 엔티티 생성 및 저장 (extra_info JSONB에 EXIF 저장)</li>
     *   <li>YOLO 결과 대기 (최대 5초) 및 최적 탐지 결과 선택 (YOLO >= 90% ? YOLO : Vision API)</li>
     *   <li>Sighting 엔티티 생성 및 저장 (EXIF 데이터 반영)</li>
     *   <li>사용자에게 즉시 응답 (speciesProcessingStatus: PENDING)</li>
     *   <li>SpeciesProcessingEvent 발행 → 백그라운드에서 Species 생성</li>
     * </ol>
     *
     * @param imageFile 업로드된 이미지 파일
     * @param user 현재 로그인한 사용자
     * @param title Sighting 제목 (선택)
     * @param description Sighting 설명 (선택)
     * @return 생성된 Sighting 엔티티와 Vision API 결과
     */
    @Transactional
    public SightingCreateResult createSighting(
        MultipartFile imageFile,
        User user,
        String title,
        String description
    ) {
        log.info("Creating Sighting for user: {} with image: {}", user.getEmail(), imageFile.getOriginalFilename());

        // 1. EXIF 메타데이터 추출
        ExifMetadata exifMetadata = exifService.extractMetadata(imageFile);
        log.info("EXIF metadata extracted: GPS={}, capturedAt={}, camera={} {}",
            exifMetadata.getGpsLocation() != null,
            exifMetadata.getCapturedAt(),
            exifMetadata.getCameraMake(),
            exifMetadata.getCameraModel());

        // 2. EXIF 제거 이미지 생성
        byte[] sanitizedImageBytes = exifRemovalService.removeExifMetadata(imageFile);
        log.info("EXIF removed from image, sanitized size: {} bytes", sanitizedImageBytes.length);

        // 3. S3에 두 버전 업로드 (원본 + EXIF 제거)
        S3Service.ImageUploadResult uploadResult = s3Service.uploadBothVersions(imageFile, sanitizedImageBytes);
        log.info("Images uploaded to S3 - Original: {}, Sanitized: {}",
            uploadResult.getOriginalUrl(), uploadResult.getSanitizedUrl());

        // 4. Vision API로 동물 인식 (신뢰도 0.5 이상, 최대 5개)
        List<AnimalDetection> detections = animalVisionService.detectAnimals(imageFile, 0.5f, 5);

        // 5. 동물이 감지되지 않은 경우 예외 발생
        if (detections.isEmpty()) {
            log.warn("No animals detected in image: {}", imageFile.getOriginalFilename());
            throw new SightingException(SightingErrorCode.NO_ANIMAL_DETECTED);
        }

        // 6. FastAPI YOLO 추론 요청 (비동기)
        String yoloRequestId = requestYoloInference(uploadResult.getSanitizedUrl());

        // 7. Media 엔티티 생성 및 저장
        Media media = createMediaEntity(user, imageFile, uploadResult, exifMetadata);

        // 8. AiDetection 엔티티 저장 - Vision API 결과 (모든 감지 결과 저장)
        saveAiDetections(media, detections, "VISION");

        // 9. YOLO 결과 대기 및 처리 (최대 5초)
        AnimalDetection topDetection = selectBestDetection(detections, yoloRequestId, media);
        log.info("Final top detection: {} (confidence: {}, scientificName: {})",
            topDetection.getLabel(), topDetection.getConfidence(), topDetection.getScientificName());

        // 10. Sighting 엔티티 생성 및 저장
        Sighting sighting = createSightingEntity(
            user,
            media,
            exifMetadata,
            title,
            description,
            topDetection.getLabel(),
            topDetection.getConfidence()
        );

        // 11. Species 처리 이벤트 발행 (비동기 처리)
        if (topDetection.getScientificName() != null && !topDetection.getScientificName().isEmpty()) {
            publishSpeciesProcessingEvent(sighting, topDetection);
            log.info("Species processing event published for Sighting ID: {}", sighting.getId());
        } else {
            log.warn("No scientific name found for detection: {}. Skipping Species processing.", topDetection.getLabel());
        }

        // 12. 사용자에게 즉시 응답 반환 (Species 처리는 백그라운드)
        return SightingCreateResult.of(sighting, media, detections, true);
    }

    /**
     * Sighting 수정
     *
     * @param sightingId Sighting ID
     * @param actorId 요청한 사용자 ID
     * @param isAdmin 관리자 여부
     * @param request 수정 요청 데이터
     * @return 수정된 Sighting 정보
     */
    @Transactional
    public SightingUpdateResponse updateSighting(
        UUID sightingId,
        UUID actorId,
        boolean isAdmin,
        SightingUpdateRequest request
    ) {
        // Sighting 조회
        Sighting sighting = sightingRepository.findById(sightingId)
            .orElseThrow(() -> new SightingException(SightingErrorCode.NOT_FOUND));

        // 권한 검증 (소유자 또는 ADMIN)
        if (!isAdmin && !sighting.getUser().getId().equals(actorId)) {
            throw new SightingException(SightingErrorCode.FORBIDDEN);
        }

        // 엔티티 업데이트
        sighting.update(request);

        // 저장 (updated_at은 @PreUpdate로 자동 설정됨)
        Sighting updated = sightingRepository.save(sighting);

        log.info("Sighting updated: {} by user: {}", sightingId, actorId);

        return SightingUpdateResponse.of(
            updated.getId(),
            updated.getTitle(),
            updated.getDescription(),
            updated.getVisibility().name(),
            updated.getOccurredAt(),
            updated.getAddressText(),
            updated.getUpdatedAt()
        );
    }

    /**
     * Sighting 삭제
     *
     * @param sightingId 삭제할 Sighting ID
     * @param actorId 요청자 ID
     * @param isAdmin ADMIN 권한 여부
     * @return SightingDeleteResponse
     */
    @Transactional
    public SightingDeleteResponse deleteSighting(
        UUID sightingId,
        UUID actorId,
        boolean isAdmin
    ) {
        // Sighting 조회
        Sighting sighting = sightingRepository.findById(sightingId)
            .orElseThrow(() -> new SightingException(SightingErrorCode.NOT_FOUND));

        // 권한 검증 (소유자 또는 ADMIN)
        if (!isAdmin && !sighting.getUser().getId().equals(actorId)) {
            throw new SightingException(SightingErrorCode.FORBIDDEN);
        }

        // Sighting 삭제
        sightingRepository.delete(sighting);

        log.info("Sighting deleted: {} by user: {}", sightingId, actorId);

        return SightingDeleteResponse.of(sightingId);
    }

    // ========== Private Helper Methods ==========

    /**
     * Entity 속성명을 DB 컬럼명으로 변환
     * (camelCase -> snake_case)
     */
    private String convertPropertyToColumn(String property) {
        return switch (property) {
            case "createdAt" -> "created_at";
            case "occurredAt" -> "occurred_at";
            default -> "created_at";
        };
    }

    private SightingListItemDto mapListRow(SightingListRow r) {
        return new SightingListItemDto(
            r.getId(),
            r.getTitle(),
            r.getDescription(),
            r.getOccurredAt(),
            r.getDetectedBy(),
            r.getAiConfidence(),
            r.getVisibility(),
            r.getIsVerified(),
            r.getCreatedAt(),
            r.getUpdatedAt(),
            r.getDisplayName(),
            r.getCommonNameKo(),
            r.getCommonNameEn(),
            r.getScientificName(),
            r.getStatus(),
            r.getSanitizedUrl(),
            r.getGeom()
        );
    }

    private SightingDetailResponse mapDetailRow(SightingDetailRow r, UUID viewerId) {
        // User 정보
        SightingDetailResponse.UserInfo userInfo = new SightingDetailResponse.UserInfo(
            r.getUserId(),
            r.getDisplayName(),
            r.getUserEmail()
        );

        // Species 정보 (없을 수 있음)
        SightingDetailResponse.SpeciesInfo speciesInfo = null;
        if (r.getSpeciesId() != null) {
            speciesInfo = new SightingDetailResponse.SpeciesInfo(
                r.getSpeciesId(),
                r.getCommonNameKo(),
                r.getCommonNameEn(),
                r.getScientificName(),
                r.getStatus()
            );
        }

        // EXIF 정보
        SightingDetailResponse.ExifInfo exifInfo = new SightingDetailResponse.ExifInfo(
            r.getCameraMake(),
            r.getCameraModel(),
            r.getCapturedAt(),
            r.getGpsLatitude(),
            r.getGpsLongitude()
        );

        // Media 정보 (없을 수 있음)
        SightingDetailResponse.MediaInfo mediaInfo = null;
        if (r.getMediaId() != null) {
            // 소유자인 경우에만 storagePath(원본) 제공
            boolean isOwner = viewerId != null && viewerId.equals(r.getUserId());
            String storagePath = isOwner ? r.getStoragePath() : null;

            mediaInfo = new SightingDetailResponse.MediaInfo(
                r.getMediaId(),
                r.getSanitizedUrl(),
                storagePath,
                r.getMimeType(),
                r.getBytes(),
                r.getWidth(),
                r.getHeight(),
                exifInfo
            );
        }

        return new SightingDetailResponse(
            r.getId(),
            r.getTitle(),
            r.getDescription(),
            r.getOccurredAt(),
            r.getDetectedBy(),
            r.getAiConfidence(),
            r.getVisibility(),
            r.getIsVerified(),
            r.getAddressText(),
            r.getCreatedAt(),
            r.getUpdatedAt(),
            userInfo,
            speciesInfo,
            mediaInfo,
            r.getGeom()
        );
    }

    /**
     * Media 엔티티 생성 및 저장
     *
     * @param user 사용자
     * @param imageFile 이미지 파일
     * @param uploadResult S3 업로드 결과 (원본 URL, 공개 URL)
     * @param exifMetadata EXIF 메타데이터
     * @return 저장된 Media 엔티티
     */
    private Media createMediaEntity(
        User user,
        MultipartFile imageFile,
        S3Service.ImageUploadResult uploadResult,
        ExifMetadata exifMetadata
    ) {
        Media media = new Media();
        media.setUser(user);
        media.setStoragePath(uploadResult.getOriginalUrl());      // 원본 이미지 (EXIF 포함)
        media.setMimeType(imageFile.getContentType());
        media.setBytes(imageFile.getSize());
        media.setWidth(exifMetadata.getWidth());
        media.setHeight(exifMetadata.getHeight());

        // EXIF 메타데이터 및 공개 URL을 extra_info JSONB에 저장
        java.util.Map<String, Object> extraInfo = new java.util.HashMap<>();

        // 공개 이미지 URL (EXIF 제거됨) - 원본 URL과 다른 파일명으로 보안 강화
        extraInfo.put("sanitizedUrl", uploadResult.getSanitizedUrl());

        // EXIF 메타데이터
        if (exifMetadata.getCameraMake() != null) {
            extraInfo.put("cameraMake", exifMetadata.getCameraMake());
        }
        if (exifMetadata.getCameraModel() != null) {
            extraInfo.put("cameraModel", exifMetadata.getCameraModel());
        }
        if (exifMetadata.getCapturedAt() != null) {
            extraInfo.put("capturedAt", exifMetadata.getCapturedAt().toString());
        }
        if (exifMetadata.getGpsLocation() != null) {
            extraInfo.put("gpsLatitude", exifMetadata.getGpsLocation().getY());
            extraInfo.put("gpsLongitude", exifMetadata.getGpsLocation().getX());
        }

        media.setExtraInfo(extraInfo);

        Media saved = mediaRepository.save(media);
        log.info("Media created with ID: {} for user: {} (original: {}, sanitized: {})",
            saved.getId(), user.getEmail(), uploadResult.getOriginalUrl(), uploadResult.getSanitizedUrl());

        return saved;
    }

    /**
     * Sighting 엔티티 생성 및 저장
     *
     * @param user 사용자
     * @param media Media 엔티티
     * @param exifMetadata EXIF 메타데이터
     * @param title 제목
     * @param description 설명
     * @param animalLabel 동물 명칭 (Vision API 결과)
     * @param confidence 신뢰도
     * @return 저장된 Sighting 엔티티
     */
    private Sighting createSightingEntity(
        User user,
        Media media,
        ExifMetadata exifMetadata,
        String title,
        String description,
        String animalLabel,
        Float confidence
    ) {
        Sighting sighting = new Sighting();
        sighting.setUser(user);
        sighting.setMedia(media);
        sighting.setTitle(title != null ? title : animalLabel);
        sighting.setDescription(description);
        sighting.setDetectedBy(DetectedBy.AI);
        sighting.setAiConfidence(confidence != null ? BigDecimal.valueOf(confidence) : null);
        sighting.setVisibility(Visibility.PUBLIC);
        sighting.setIsVerified(false);

        // EXIF GPS 정보가 있으면 설정
        if (exifMetadata.getGpsLocation() != null) {
            sighting.setGeom(exifMetadata.getGpsLocation());
            log.debug("GPS location set for Sighting: {}", exifMetadata.getGpsLocation());
        }

        // EXIF 촬영 시간이 있으면 설정
        if (exifMetadata.getCapturedAt() != null) {
            sighting.setOccurredAt(exifMetadata.getCapturedAt());
            log.debug("Occurred at set from EXIF: {}", exifMetadata.getCapturedAt());
        }

        Sighting saved = sightingRepository.save(sighting);
        log.info("Sighting created with ID: {} for user: {}", saved.getId(), user.getEmail());

        return saved;
    }

    /**
     * AiDetection 엔티티 저장 (모든 감지 결과 저장)
     *
     * @param media Media 엔티티
     * @param detections AI 감지 결과
     * @param source 감지 소스 ("VISION" 또는 "YOLO")
     */
    private void saveAiDetections(Media media, List<AnimalDetection> detections, String source) {
        if (detections == null || detections.isEmpty()) {
            log.debug("No detections to save for Media ID: {}", media.getId());
            return;
        }

        for (AnimalDetection detection : detections) {
            AiDetection aiDetection = new AiDetection();
            aiDetection.setMedia(media);
            aiDetection.setLabel(detection.getLabel());
            aiDetection.setScore(detection.getConfidence() != null
                ? java.math.BigDecimal.valueOf(detection.getConfidence())
                : null);

            // extra_info에 scientificName, description, source 저장
            java.util.Map<String, Object> extraInfo = new java.util.HashMap<>();
            extraInfo.put("source", source);
            if (detection.getScientificName() != null) {
                extraInfo.put("scientificName", detection.getScientificName());
            }
            if (detection.getDescription() != null) {
                extraInfo.put("description", detection.getDescription());
            }
            aiDetection.setExtraInfo(extraInfo);

            // Bounding box 설정 (YOLO의 경우 있을 수 있음)
            if (detection.getBoundingBox() != null) {
                aiDetection.setXMin(detection.getBoundingBox().getX1() != null
                    ? BigDecimal.valueOf(detection.getBoundingBox().getX1()) : null);
                aiDetection.setYMin(detection.getBoundingBox().getY1() != null
                    ? BigDecimal.valueOf(detection.getBoundingBox().getY1()) : null);
                aiDetection.setXMax(detection.getBoundingBox().getX2() != null
                    ? BigDecimal.valueOf(detection.getBoundingBox().getX2()) : null);
                aiDetection.setYMax(detection.getBoundingBox().getY2() != null
                    ? BigDecimal.valueOf(detection.getBoundingBox().getY2()) : null);
            } else {
                aiDetection.setXMin(null);
                aiDetection.setYMin(null);
                aiDetection.setXMax(null);
                aiDetection.setYMax(null);
            }

            aiDetectionRepository.save(aiDetection);
        }

        log.info("Saved {} {} AI detections for Media ID: {}", detections.size(), source, media.getId());
    }

    /**
     * FastAPI YOLO 추론 요청
     *
     * @param sanitizedImageUrl EXIF가 제거된 이미지 URL
     * @return YOLO 요청 ID (실패 시 null)
     */
    private String requestYoloInference(String sanitizedImageUrl) {
        try {
            com.example.demo.domain.dto.yolo.YoloInferResponse response =
                yoloInferenceService.requestInference(java.util.List.of(sanitizedImageUrl));
            log.info("YOLO inference requested: requestId={}", response.getRequestId());
            return response.getRequestId();
        } catch (Exception e) {
            log.warn("Failed to request YOLO inference: {}. Continuing with Vision API result only.", e.getMessage());
            return null;
        }
    }

    /**
     * Vision API와 YOLO 결과를 비교하여 최적의 탐지 결과 선택
     *
     * @param visionDetections Vision API 탐지 결과 (신뢰도 순 정렬됨)
     * @param yoloRequestId YOLO 요청 ID
     * @param media Media 엔티티 (YOLO 결과 저장용)
     * @return 최종 선택된 탐지 결과
     */
    private AnimalDetection selectBestDetection(
        List<AnimalDetection> visionDetections,
        String yoloRequestId,
        Media media
    ) {
        AnimalDetection visionTopDetection = visionDetections.get(0);

        // YOLO 요청이 실패했거나 없으면 Vision API 결과 사용
        if (yoloRequestId == null) {
            log.info("Using Vision API result (YOLO not available): {} ({})",
                visionTopDetection.getLabel(), visionTopDetection.getConfidence());
            return visionTopDetection;
        }

        // YOLO 결과 대기 (최대 5초)
        com.example.demo.domain.dto.yolo.YoloCallbackRequest yoloResult = waitForYoloResult(yoloRequestId, 5000);

        if (yoloResult == null || !"success".equals(yoloResult.getStatus())) {
            log.info("Using Vision API result (YOLO timeout or error): {} ({})",
                visionTopDetection.getLabel(), visionTopDetection.getConfidence());
            return visionTopDetection;
        }

        // YOLO 결과를 AnimalDetection 리스트로 변환
        List<AnimalDetection> yoloDetections = convertYoloToAnimalDetections(yoloResult);

        if (yoloDetections.isEmpty()) {
            log.info("Using Vision API result (YOLO no detection): {} ({})",
                visionTopDetection.getLabel(), visionTopDetection.getConfidence());
            return visionTopDetection;
        }

        // YOLO 탐지 결과를 DB에 저장
        saveAiDetections(media, yoloDetections, "YOLO");

        // 가장 신뢰도 높은 YOLO 탐지 추출
        AnimalDetection yoloTopDetection = yoloDetections.get(0);

        // YOLO 신뢰도가 임계값 이상이면 YOLO 결과 사용
        if (yoloTopDetection.getConfidence() >= yoloConfidenceThreshold) {
            log.info("Using YOLO result (confidence >= {}): {} ({})",
                yoloConfidenceThreshold, yoloTopDetection.getLabel(), yoloTopDetection.getConfidence());
            return yoloTopDetection;
        }

        // 그 외에는 Vision API 결과 사용
        log.info("Using Vision API result (YOLO confidence < {}): {} ({}) vs YOLO: {} ({})",
            yoloConfidenceThreshold,
            visionTopDetection.getLabel(), visionTopDetection.getConfidence(),
            yoloTopDetection.getLabel(), yoloTopDetection.getConfidence());
        return visionTopDetection;
    }

    /**
     * YOLO 결과를 지정된 시간만큼 대기
     *
     * @param requestId YOLO 요청 ID
     * @param timeoutMs 대기 시간 (밀리초)
     * @return YOLO Callback 결과 (타임아웃 시 null)
     */
    private com.example.demo.domain.dto.yolo.YoloCallbackRequest waitForYoloResult(String requestId, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        long pollInterval = 200; // 200ms마다 체크

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            com.example.demo.domain.dto.yolo.YoloCallbackRequest result = yoloCallbackService.getResult(requestId);
            if (result != null) {
                yoloCallbackService.removeResult(requestId);
                return result;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("YOLO result wait interrupted");
                return null;
            }
        }

        log.warn("YOLO result timeout after {}ms for requestId: {}", timeoutMs, requestId);
        return null;
    }

    /**
     * YOLO Callback 결과를 AnimalDetection 리스트로 변환
     *
     * @param yoloResult YOLO Callback 결과
     * @return AnimalDetection 리스트 (신뢰도 높은 순으로 정렬)
     */
    private List<AnimalDetection> convertYoloToAnimalDetections(com.example.demo.domain.dto.yolo.YoloCallbackRequest yoloResult) {
        if (yoloResult.getResults() == null || yoloResult.getResults().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        com.example.demo.domain.dto.yolo.YoloCallbackRequest.Result firstResult = yoloResult.getResults().get(0);
        if (firstResult.getDetections() == null || firstResult.getDetections().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 모든 YOLO 탐지 결과를 AnimalDetection으로 변환
        List<AnimalDetection> detections = firstResult.getDetections().stream()
            .map(detection -> {
                // Bounding box 변환 (YOLO의 Float -> Vision의 Integer)
                com.example.demo.domain.dto.vision.BoundingBox bbox = null;
                if (detection.getBbox() != null) {
                    bbox = com.example.demo.domain.dto.vision.BoundingBox.builder()
                        .x1(detection.getBbox().getXMin() != null ? detection.getBbox().getXMin().intValue() : null)
                        .y1(detection.getBbox().getYMin() != null ? detection.getBbox().getYMin().intValue() : null)
                        .x2(detection.getBbox().getXMax() != null ? detection.getBbox().getXMax().intValue() : null)
                        .y2(detection.getBbox().getYMax() != null ? detection.getBbox().getYMax().intValue() : null)
                        .build();
                }

                // AnimalDetection으로 변환
                return AnimalDetection.of(
                    detection.getLabel(),
                    detection.getConfidence(),
                    detection.getLabel(),
                    detection.getLabel(),  // YOLO는 학명을 제공하지 않으므로 label을 scientificName으로 사용
                    bbox
                );
            })
            .sorted(java.util.Comparator.comparing(AnimalDetection::getConfidence).reversed())
            .collect(java.util.stream.Collectors.toList());

        log.info("Converted {} YOLO detections to AnimalDetections", detections.size());
        return detections;
    }

    /**
     * Species 처리 이벤트 발행
     *
     * @param sighting 생성된 Sighting
     * @param detection Vision API 결과
     */
    private void publishSpeciesProcessingEvent(Sighting sighting, AnimalDetection detection) {
        SpeciesProcessingEvent event = new SpeciesProcessingEvent(
            this,
            sighting.getId(),
            detection.getLabel(),
            detection.getScientificName(),
            detection.getConfidence()
        );
        eventPublisher.publishEvent(event);
    }

    /**
     * Sighting 생성 결과를 담는 내부 클래스
     */
    @lombok.Value(staticConstructor = "of")
    public static class SightingCreateResult {
        Sighting sighting;
        Media media;
        List<AnimalDetection> detections;
        boolean speciesProcessing;  // Species 처리 여부 (true: PENDING, false: NOT_DETECTED)
    }
}
