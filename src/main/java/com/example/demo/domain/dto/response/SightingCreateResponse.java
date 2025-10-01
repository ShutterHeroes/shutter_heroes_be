package com.example.demo.domain.dto.response;

import com.example.demo.domain.dto.vision.AnimalDetection;
import com.example.demo.domain.entity.Media;
import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.enums.SpeciesProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Sighting 생성 응답 DTO
 *
 * <p><b>응답 정보:</b></p>
 * <ul>
 *   <li>생성된 Sighting 정보</li>
 *   <li>업로드된 Media 정보 (S3 URL, EXIF 메타데이터)</li>
 *   <li>Vision API 동물 인식 결과 (최대 5개)</li>
 *   <li>Species 처리 상태 (PENDING/COMPLETED/NOT_DETECTED)</li>
 * </ul>
 *
 * <p><b>Species 처리 상태:</b></p>
 * <ul>
 *   <li>PENDING: 백그라운드에서 Species 처리 중</li>
 *   <li>COMPLETED: Species 처리 완료 (이미 DB에 존재하는 경우)</li>
 *   <li>NOT_DETECTED: 동물이 인식되지 않음</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SightingCreateResponse {

    /**
     * 생성된 Sighting의 ID (UUID)
     */
    private UUID sightingId;

    /**
     * Sighting 제목
     */
    private String title;

    /**
     * Sighting 설명
     */
    private String description;

    /**
     * AI 신뢰도 (0.0 ~ 1.0)
     */
    private BigDecimal aiConfidence;

    /**
     * GPS 위치 정보 (PostGIS Point - EXIF에서 추출)
     */
    private Point gpsLocation;

    /**
     * 촬영 시간 (EXIF DateTimeOriginal)
     */
    private LocalDateTime occurredAt;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * Media 정보
     */
    private MediaInfo media;

    /**
     * Vision API 동물 인식 결과 (최대 5개, 신뢰도 높은 순)
     */
    private List<AnimalDetection> detections;

    /**
     * Species 처리 상태
     * - PENDING: 백그라운드에서 Species 처리 중
     * - COMPLETED: Species 처리 완료
     * - NOT_DETECTED: 동물이 인식되지 않음
     */
    private SpeciesProcessingStatus speciesProcessingStatus;

    /**
     * Media 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaInfo {
        private UUID mediaId;
        private String url;
        private String mimeType;
        private Integer width;
        private Integer height;
        private Long bytes;
        private String cameraMake;
        private String cameraModel;
        private LocalDateTime capturedAt;
    }

    /**
     * Sighting 엔티티와 Media, Vision 결과로부터 응답 DTO 생성
     *
     * @param sighting 생성된 Sighting 엔티티
     * @param media 생성된 Media 엔티티
     * @param detections Vision API 결과
     * @param speciesProcessing Species 처리 여부
     * @return SightingCreateResponse
     */
    public static SightingCreateResponse of(Sighting sighting, Media media, List<AnimalDetection> detections, boolean speciesProcessing) {
        SpeciesProcessingStatus status;
        if (detections.isEmpty()) {
            status = SpeciesProcessingStatus.NOT_DETECTED;
        } else if (speciesProcessing) {
            status = SpeciesProcessingStatus.PENDING;
        } else {
            status = SpeciesProcessingStatus.NOT_DETECTED;
        }

        MediaInfo mediaInfo = MediaInfo.builder()
            .mediaId(media.getId())
            .url(media.getStoragePath())
            .mimeType(media.getMimeType())
            .width(media.getWidth())
            .height(media.getHeight())
            .bytes(media.getBytes())
            .cameraMake(media.getCameraMake())
            .cameraModel(media.getCameraModel())
            .capturedAt(media.getCapturedAt())
            .build();

        return SightingCreateResponse.builder()
            .sightingId(sighting.getId())
            .title(sighting.getTitle())
            .description(sighting.getDescription())
            .aiConfidence(sighting.getAiConfidence())
            .gpsLocation(sighting.getGeom())
            .occurredAt(sighting.getOccurredAt())
            .createdAt(sighting.getCreatedAt())
            .media(mediaInfo)
            .detections(detections)
            .speciesProcessingStatus(status)
            .build();
    }
}
