package com.example.demo.domain.dto.response;

import com.example.demo.domain.dto.vision.AnimalDetection;
import com.example.demo.domain.entity.Media;
import com.example.demo.domain.entity.Sighting;
import com.example.demo.domain.enums.SpeciesProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
     * GPS 위치 정보 (위도, 경도)
     */
    private GpsLocation gpsLocation;

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
     * GPS 위치 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsLocation {
        private Double latitude;   // 위도 (Y)
        private Double longitude;  // 경도 (X)
    }

    /**
     * Media 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaInfo {
        private UUID mediaId;
        private String url;                     // 공개 이미지 URL (EXIF 제거됨)
        private String originalUrl;             // 원본 이미지 URL (EXIF 포함, 관리자용)
        private String mimeType;
        private Integer width;
        private Integer height;
        private Long bytes;
        private java.util.Map<String, Object> extraInfo;  // EXIF 메타데이터 (JSONB)
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

        // extra_info에서 sanitizedUrl 추출
        String sanitizedUrl = null;
        if (media.getExtraInfo() != null && media.getExtraInfo().containsKey("sanitizedUrl")) {
            sanitizedUrl = (String) media.getExtraInfo().get("sanitizedUrl");
        }

        MediaInfo mediaInfo = MediaInfo.builder()
            .mediaId(media.getId())
            .url(sanitizedUrl)                    // 공개 이미지 URL (EXIF 제거, extra_info에서 추출)
            .originalUrl(media.getStoragePath())  // 원본 이미지 URL (EXIF 포함)
            .mimeType(media.getMimeType())
            .width(media.getWidth())
            .height(media.getHeight())
            .bytes(media.getBytes())
            .extraInfo(media.getExtraInfo())      // EXIF 메타데이터 및 sanitizedUrl (JSONB)
            .build();

        // GPS 위치 정보 변환 (PostGIS Point -> GpsLocation DTO)
        GpsLocation gpsLocation = null;
        if (sighting.getGeom() != null) {
            gpsLocation = GpsLocation.builder()
                .latitude(sighting.getGeom().getY())   // 위도
                .longitude(sighting.getGeom().getX())  // 경도
                .build();
        }

        return SightingCreateResponse.builder()
            .sightingId(sighting.getId())
            .title(sighting.getTitle())
            .description(sighting.getDescription())
            .aiConfidence(sighting.getAiConfidence())
            .gpsLocation(gpsLocation)
            .occurredAt(sighting.getOccurredAt())
            .createdAt(sighting.getCreatedAt())
            .media(mediaInfo)
            .detections(detections)
            .speciesProcessingStatus(status)
            .build();
    }
}
