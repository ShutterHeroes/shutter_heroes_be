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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    private final S3Service s3Service;
    private final ExifService exifService;
    private final ExifRemovalService exifRemovalService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 이미지를 업로드하고 Sighting을 생성합니다.
     *
     * <p><b>동작 방식:</b></p>
     * <ol>
     *   <li>EXIF 메타데이터 추출 (GPS, 촬영시간, 카메라정보)</li>
     *   <li>EXIF 제거 이미지 생성</li>
     *   <li>S3에 두 버전 업로드 (원본 + EXIF 제거)</li>
     *   <li>Vision API로 동물 인식 (가장 신뢰도 높은 동물 선택)</li>
     *   <li>Media 엔티티 생성 및 저장 (extra_info JSONB에 EXIF 저장)</li>
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

        // 5. Media 엔티티 생성 및 저장
        Media media = createMediaEntity(user, imageFile, uploadResult, exifMetadata);

        // 6. AiDetection 엔티티 저장 (모든 감지 결과 저장)
        saveAiDetections(media, detections);

        if (detections.isEmpty()) {
            log.warn("No animals detected in image: {}", imageFile.getOriginalFilename());
            // 동물이 인식되지 않은 경우에도 Sighting 생성 (Species 없음)
            Sighting sighting = createSightingEntity(user, media, exifMetadata, title, description, null, null);
            return SightingCreateResult.of(sighting, media, detections, false);
        }

        // 7. 가장 신뢰도 높은 동물 선택 (이미 신뢰도 순으로 정렬됨)
        AnimalDetection topDetection = detections.get(0);
        log.info("Top detection: {} (confidence: {}, scientificName: {})",
            topDetection.getLabel(), topDetection.getConfidence(), topDetection.getScientificName());

        // 8. Sighting 엔티티 생성 및 저장
        Sighting sighting = createSightingEntity(
            user,
            media,
            exifMetadata,
            title,
            description,
            topDetection.getLabel(),
            topDetection.getConfidence()
        );

        // 9. Species 처리 이벤트 발행 (비동기 처리)
        if (topDetection.getScientificName() != null && !topDetection.getScientificName().isEmpty()) {
            publishSpeciesProcessingEvent(sighting, topDetection);
            log.info("Species processing event published for Sighting ID: {}", sighting.getId());
        } else {
            log.warn("No scientific name found for detection: {}. Skipping Species processing.", topDetection.getLabel());
        }

        // 10. 사용자에게 즉시 응답 반환 (Species 처리는 백그라운드)
        return SightingCreateResult.of(sighting, media, detections, true);
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
     * @param detections Vision API 감지 결과
     */
    private void saveAiDetections(Media media, List<AnimalDetection> detections) {
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

            // extra_info에 scientificName, description 저장
            java.util.Map<String, Object> extraInfo = new java.util.HashMap<>();
            if (detection.getScientificName() != null) {
                extraInfo.put("scientificName", detection.getScientificName());
            }
            if (detection.getDescription() != null) {
                extraInfo.put("description", detection.getDescription());
            }
            aiDetection.setExtraInfo(extraInfo);

            // Bounding box는 null로 설정 (향후 Object Detection 추가 시 사용)
            aiDetection.setXMin(null);
            aiDetection.setYMin(null);
            aiDetection.setXMax(null);
            aiDetection.setYMax(null);

            aiDetectionRepository.save(aiDetection);
        }

        log.info("Saved {} AI detections for Media ID: {}", detections.size(), media.getId());
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
     * Sighting 전체 목록 조회 (페이징, 검색)
     *
     * @param viewerIdNullable 로그인 사용자 ID (비로그인 시 null)
     * @param keyword 검색어 (학명 또는 한국어 이름, null이면 전체 조회)
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

    /**
     * Sighting 상세 조회
     *
     * @param sightingId Sighting ID
     * @param viewerIdNullable 로그인 사용자 ID (비로그인 시 null)
     * @return Sighting 상세 정보
     * @throws ResponseStatusException NOT_FOUND (404) - Sighting이 존재하지 않거나 권한 없음
     */
    @Transactional(readOnly = true)
    public SightingDetailResponse findSightingDetail(UUID sightingId, UUID viewerIdNullable) {
        SightingDetailRow row = sightingRepository.findDetailById(sightingId, viewerIdNullable)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Sighting not found or you don't have permission to access it"
            ));

        return mapDetailRow(row, viewerIdNullable);
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
     * Sighting 수정
     *
     * @param sightingId Sighting ID
     * @param actorId 요청한 사용자 ID
     * @param isAdmin 관리자 여부
     * @param request 수정 요청 데이터
     * @return 수정된 Sighting 정보
     * @throws ResponseStatusException NOT_FOUND (404) - Sighting이 존재하지 않음
     * @throws ResponseStatusException FORBIDDEN (403) - 수정 권한 없음
     * @throws ResponseStatusException BAD_REQUEST (400) - 잘못된 visibility 값
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
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Sighting not found"
            ));

        // 권한 검증 (소유자 또는 ADMIN)
        if (!isAdmin && !sighting.getUser().getId().equals(actorId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You don't have permission to update this sighting"
            );
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
     * @throws ResponseStatusException NOT_FOUND (404) - Sighting이 존재하지 않음
     * @throws ResponseStatusException FORBIDDEN (403) - 삭제 권한 없음
     */
    @Transactional
    public SightingDeleteResponse deleteSighting(
        UUID sightingId,
        UUID actorId,
        boolean isAdmin
    ) {
        // Sighting 조회
        Sighting sighting = sightingRepository.findById(sightingId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Sighting not found"
            ));

        // 권한 검증 (소유자 또는 ADMIN)
        if (!isAdmin && !sighting.getUser().getId().equals(actorId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You don't have permission to delete this sighting"
            );
        }

        // Sighting 삭제
        sightingRepository.delete(sighting);

        log.info("Sighting deleted: {} by user: {}", sightingId, actorId);

        return SightingDeleteResponse.of(sightingId);
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
