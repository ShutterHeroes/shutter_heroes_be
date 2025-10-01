package com.example.demo.domain.service;

import com.example.demo.domain.dto.exif.ExifMetadata;
import com.example.demo.domain.dto.vision.AnimalDetection;
import com.example.demo.domain.entity.Media;
import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.DetectedBy;
import com.example.demo.domain.enums.Visibility;
import com.example.demo.domain.event.SpeciesProcessingEvent;
import com.example.demo.domain.repository.MediaRepository;
import com.example.demo.domain.repository.SightingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

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

        if (detections.isEmpty()) {
            log.warn("No animals detected in image: {}", imageFile.getOriginalFilename());
            // 동물이 인식되지 않은 경우에도 Sighting 생성 (Species 없음)
            Sighting sighting = createSightingEntity(user, media, exifMetadata, title, description, null, null);
            return SightingCreateResult.of(sighting, media, detections, false);
        }

        // 6. 가장 신뢰도 높은 동물 선택 (이미 신뢰도 순으로 정렬됨)
        AnimalDetection topDetection = detections.get(0);
        log.info("Top detection: {} (confidence: {}, scientificName: {})",
            topDetection.getLabel(), topDetection.getConfidence(), topDetection.getScientificName());

        // 7. Sighting 엔티티 생성 및 저장
        Sighting sighting = createSightingEntity(
            user,
            media,
            exifMetadata,
            title,
            description,
            topDetection.getLabel(),
            topDetection.getConfidence()
        );

        // 8. Species 처리 이벤트 발행 (비동기 처리)
        if (topDetection.getScientificName() != null && !topDetection.getScientificName().isEmpty()) {
            publishSpeciesProcessingEvent(sighting, topDetection);
            log.info("Species processing event published for Sighting ID: {}", sighting.getId());
        } else {
            log.warn("No scientific name found for detection: {}. Skipping Species processing.", topDetection.getLabel());
        }

        // 9. 사용자에게 즉시 응답 반환 (Species 처리는 백그라운드)
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
