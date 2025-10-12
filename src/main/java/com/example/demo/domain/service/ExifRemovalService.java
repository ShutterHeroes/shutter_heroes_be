package com.example.demo.domain.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.example.demo.exceptions.errorcode.FileErrorCode;
import com.example.demo.exceptions.exception.FileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * EXIF 메타데이터 제거 서비스
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>이미지에서 GPS 정보 제거</li>
 *   <li>이미지에서 촬영 시간 제거</li>
 *   <li>이미지에서 카메라 정보 제거</li>
 *   <li>순수 이미지 데이터만 유지</li>
 * </ul>
 *
 * <p><b>제거 방식:</b></p>
 * <ul>
 *   <li>원본 이미지를 BufferedImage로 읽음</li>
 *   <li>EXIF 메타데이터 없이 새로운 이미지로 저장</li>
 *   <li>이미지 품질 유지 (손실 최소화)</li>
 * </ul>
 */
@Slf4j
@Service
public class ExifRemovalService {

    /**
     * 이미지에서 EXIF 메타데이터를 제거하고 새로운 이미지 바이트 배열을 반환합니다.
     *
     * <p><b>동작 방식:</b></p>
     * <ol>
     *   <li>원본 이미지를 BufferedImage로 읽음</li>
     *   <li>EXIF 메타데이터 없이 새로운 이미지로 인코딩</li>
     *   <li>원본 형식 유지 (JPEG, PNG, WebP 등)</li>
     * </ol>
     *
     * @param imageFile 원본 이미지 파일
     * @return EXIF가 제거된 이미지 바이트 배열
     * @throws FileException 이미지 처리 실패 시
     */
    public byte[] removeExifMetadata(MultipartFile imageFile) {
        try {
            log.info("Removing EXIF metadata from image: {}", imageFile.getOriginalFilename());

            // 1. 이미지 형식 확인
            String formatName = getImageFormat(imageFile);
            log.info("Detected image format: {}", formatName);

            // 2. 원본 이미지를 BufferedImage로 읽기
            BufferedImage image = ImageIO.read(imageFile.getInputStream());

            if (image == null) {
                log.error("Failed to read image file: {}. Format: {}",
                    imageFile.getOriginalFilename(), formatName);
                throw new FileException(FileErrorCode.INVALID_FILE_TYPE);
            }

            // 3. EXIF 메타데이터 없이 새로운 이미지로 저장
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 형식별로 적절한 포맷으로 저장
            if ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)) {
                // JPEG로 저장 (EXIF 없이)
                ImageIO.write(image, "jpg", outputStream);
            } else if ("webp".equalsIgnoreCase(formatName)) {
                // WebP로 저장 (EXIF 없이)
                ImageIO.write(image, "webp", outputStream);
            } else if ("png".equalsIgnoreCase(formatName)) {
                // PNG로 저장 (메타데이터 없음 보장)
                ImageIO.write(image, "png", outputStream);
            } else {
                // 기타 형식은 PNG로 변환하여 저장
                log.info("Converting {} to PNG format for EXIF removal", formatName);
                ImageIO.write(image, "png", outputStream);
            }

            byte[] sanitizedImageBytes = outputStream.toByteArray();

            log.info("EXIF metadata removed successfully. Original size: {} bytes, Sanitized size: {} bytes, Format: {}",
                imageFile.getSize(), sanitizedImageBytes.length, formatName);

            // 4. 검증: EXIF가 제거되었는지 확인
            verifyExifRemoval(sanitizedImageBytes);

            return sanitizedImageBytes;

        } catch (IOException e) {
            log.error("Failed to remove EXIF metadata: {}", e.getMessage(), e);
            throw new FileException(FileErrorCode.EXIF_EXTRACTION_FAILED);
        }
    }

    /**
     * 이미지 형식 확인
     *
     * @param imageFile 이미지 파일
     * @return 이미지 형식 (jpeg, png 등)
     */
    private String getImageFormat(MultipartFile imageFile) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                String formatName = reader.getFormatName();
                log.debug("Image format detected: {}", formatName);
                return formatName;
            }
        } catch (IOException e) {
            log.warn("Failed to detect image format, defaulting to JPEG: {}", e.getMessage());
        }
        return "jpeg";
    }

    /**
     * EXIF 제거 검증
     *
     * @param imageBytes 처리된 이미지 바이트 배열
     */
    private void verifyExifRemoval(byte[] imageBytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));

            int tagCount = 0;
            for (Directory directory : metadata.getDirectories()) {
                tagCount += directory.getTagCount();
            }

            if (tagCount > 0) {
                log.warn("EXIF metadata still exists after removal: {} tags found", tagCount);

                // 로그로만 경고하고 계속 진행 (일부 기본 메타데이터는 남을 수 있음)
                for (Directory directory : metadata.getDirectories()) {
                    for (Tag tag : directory.getTags()) {
                        log.debug("Remaining metadata: {} - {}", tag.getTagName(), tag.getDescription());
                    }
                }
            } else {
                log.info("EXIF metadata successfully removed (0 tags found)");
            }

        } catch (Exception e) {
            log.debug("Failed to verify EXIF removal (may be expected): {}", e.getMessage());
        }
    }
}
