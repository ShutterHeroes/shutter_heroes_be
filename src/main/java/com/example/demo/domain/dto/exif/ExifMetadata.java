package com.example.demo.domain.dto.exif;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

/**
 * EXIF 메타데이터 DTO
 *
 * <p><b>포함 정보:</b></p>
 * <ul>
 *   <li>GPS 위치 (latitude, longitude) - PostGIS Point 형식</li>
 *   <li>촬영 시간 (DateTimeOriginal)</li>
 *   <li>이미지 크기 (width, height)</li>
 *   <li>카메라 정보 (제조사, 모델)</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExifMetadata {

    /**
     * GPS 위치 정보 (PostGIS Point)
     * <p>좌표계: EPSG:4326 (WGS 84)</p>
     */
    private Point gpsLocation;

    /**
     * 촬영 시간 (EXIF DateTimeOriginal)
     */
    private LocalDateTime capturedAt;

    /**
     * 이미지 너비 (픽셀)
     */
    private Integer width;

    /**
     * 이미지 높이 (픽셀)
     */
    private Integer height;

    /**
     * 카메라 제조사 (예: Apple, Samsung, Canon)
     */
    private String cameraMake;

    /**
     * 카메라 모델 (예: iPhone 14 Pro, Galaxy S23)
     */
    private String cameraModel;
}
