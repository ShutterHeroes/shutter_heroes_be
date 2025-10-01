package com.example.demo.domain.service;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.example.demo.domain.dto.exif.ExifMetadata;
import com.example.demo.exceptions.errorcode.FileErrorCode;
import com.example.demo.exceptions.exception.FileException;
import com.drew.metadata.MetadataException;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.drew.imaging.ImageMetadataReader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * EXIF 메타데이터 추출 서비스
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>GPS 위치 정보 추출 (PostGIS Point 변환)</li>
 *   <li>촬영 시간 추출 (EXIF DateTimeOriginal)</li>
 *   <li>이미지 크기 추출 (width, height)</li>
 *   <li>카메라 정보 추출 (제조사, 모델)</li>
 * </ul>
 *
 * <p><b>사용 라이브러리:</b></p>
 * <ul>
 *   <li>metadata-extractor: EXIF 메타데이터 추출</li>
 *   <li>JTS (Java Topology Suite): PostGIS Point 변환</li>
 * </ul>
 */
@Slf4j
@Service
public class ExifService {

    private static final GeometryFactory GEOMETRY_FACTORY =
        new GeometryFactory(new PrecisionModel(), 4326); // EPSG:4326 (WGS 84)

    /**
     * 이미지 파일에서 EXIF 메타데이터를 추출합니다.
     *
     * <p><b>추출 정보:</b></p>
     * <ul>
     *   <li>GPS 위치 (있는 경우)</li>
     *   <li>촬영 시간 (있는 경우)</li>
     *   <li>이미지 크기</li>
     *   <li>카메라 정보 (있는 경우)</li>
     * </ul>
     *
     * @param file 이미지 파일
     * @return ExifMetadata EXIF 메타데이터
     * @throws FileException EXIF 추출 실패 시
     */
    public ExifMetadata extractMetadata(MultipartFile file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());

            return ExifMetadata.builder()
                .gpsLocation(extractGpsLocation(metadata))
                .capturedAt(extractCapturedAt(metadata))
                .width(extractWidth(metadata))
                .height(extractHeight(metadata))
                .cameraMake(extractCameraMake(metadata))
                .cameraModel(extractCameraModel(metadata))
                .build();

        } catch (IOException e) {
            log.error("Failed to read image file: {}", e.getMessage(), e);
            throw new FileException(FileErrorCode.EXIF_EXTRACTION_FAILED);
        } catch (Exception e) {
            log.error("Failed to extract EXIF metadata: {}", e.getMessage(), e);
            throw new FileException(FileErrorCode.EXIF_EXTRACTION_FAILED);
        }
    }

    /**
     * GPS 위치 정보를 PostGIS Point로 추출
     *
     * @param metadata EXIF 메타데이터
     * @return PostGIS Point (경도, 위도) 또는 null
     */
    private Point extractGpsLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

        if (gpsDirectory == null) {
            log.debug("No GPS directory found in image");
            return null;
        }

        try {
            com.drew.lang.GeoLocation geoLocation = gpsDirectory.getGeoLocation();

            if (geoLocation == null) {
                log.debug("No GPS location found in image");
                return null;
            }

            // PostGIS Point 생성 (경도, 위도 순서 주의!)
            Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(geoLocation.getLongitude(), geoLocation.getLatitude())
            );

            log.debug("GPS location extracted: lat={}, lon={}",
                geoLocation.getLatitude(), geoLocation.getLongitude());

            return point;

        } catch (Exception e) {
            log.warn("Failed to extract GPS location: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 촬영 시간 추출 (EXIF DateTimeOriginal)
     *
     * @param metadata EXIF 메타데이터
     * @return 촬영 시간 또는 null
     */
    private LocalDateTime extractCapturedAt(Metadata metadata) {
        ExifSubIFDDirectory exifSubIFDDirectory =
            metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (exifSubIFDDirectory == null) {
            log.debug("No EXIF SubIFD directory found");
            return null;
        }

        try {
            Date date = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

            if (date == null) {
                log.debug("No DateTimeOriginal found in EXIF");
                return null;
            }

            // Date를 LocalDateTime으로 변환
            LocalDateTime capturedAt = LocalDateTime.ofInstant(
                date.toInstant(),
                java.time.ZoneId.systemDefault()
            );

            log.debug("Captured at: {}", capturedAt);
            return capturedAt;

        } catch (Exception e) {
            log.warn("Failed to extract captured time: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 이미지 너비 추출
     *
     * @param metadata EXIF 메타데이터
     * @return 이미지 너비 또는 null
     */
    private Integer extractWidth(Metadata metadata) {
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);

        if (jpegDirectory != null) {
            try {
                return jpegDirectory.getImageWidth();
            } catch (MetadataException e) {
                log.warn("Failed to extract image width from JPEG directory: {}", e.getMessage());
            }
        }

        // JPEG directory가 없으면 EXIF SubIFD에서 시도
        ExifSubIFDDirectory exifSubIFDDirectory =
            metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (exifSubIFDDirectory != null) {
            Integer width = exifSubIFDDirectory.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
            if (width != null) {
                return width;
            }
        }

        log.debug("No image width found in metadata");
        return null;
    }

    /**
     * 이미지 높이 추출
     *
     * @param metadata EXIF 메타데이터
     * @return 이미지 높이 또는 null
     */
    private Integer extractHeight(Metadata metadata) {
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);

        if (jpegDirectory != null) {
            try {
                return jpegDirectory.getImageHeight();
            } catch (MetadataException e) {
                log.warn("Failed to extract image height from JPEG directory: {}", e.getMessage());
            }
        }

        // JPEG directory가 없으면 EXIF SubIFD에서 시도
        ExifSubIFDDirectory exifSubIFDDirectory =
            metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (exifSubIFDDirectory != null) {
            Integer height = exifSubIFDDirectory.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
            if (height != null) {
                return height;
            }
        }

        log.debug("No image height found in metadata");
        return null;
    }

    /**
     * 카메라 제조사 추출
     *
     * @param metadata EXIF 메타데이터
     * @return 카메라 제조사 또는 null
     */
    private String extractCameraMake(Metadata metadata) {
        ExifIFD0Directory exifIFD0Directory =
            metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        if (exifIFD0Directory == null) {
            log.debug("No EXIF IFD0 directory found");
            return null;
        }

        String make = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE);
        log.debug("Camera make: {}", make);
        return make;
    }

    /**
     * 카메라 모델 추출
     *
     * @param metadata EXIF 메타데이터
     * @return 카메라 모델 또는 null
     */
    private String extractCameraModel(Metadata metadata) {
        ExifIFD0Directory exifIFD0Directory =
            metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        if (exifIFD0Directory == null) {
            log.debug("No EXIF IFD0 directory found");
            return null;
        }

        String model = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
        log.debug("Camera model: {}", model);
        return model;
    }
}
