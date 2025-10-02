package com.example.demo.domain.service;

import com.example.demo.config.S3Config;
import com.example.demo.exceptions.errorcode.FileErrorCode;
import com.example.demo.exceptions.exception.FileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * AWS S3 파일 업로드/삭제를 담당하는 서비스
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>원본 이미지 S3 업로드 (EXIF 포함)</li>
 *   <li>공개 이미지 S3 업로드 (EXIF 제거)</li>
 *   <li>고유한 파일명 생성 (UUID + timestamp)</li>
 *   <li>S3 URL 반환</li>
 *   <li>파일 삭제</li>
 * </ul>
 *
 * <p><b>파일 경로 구조:</b></p>
 * <ul>
 *   <li>원본: sightings/{year}/{month}/original/{uuid}_{timestamp}_{originalFilename}</li>
 *   <li>공개: sightings/{year}/{month}/sanitized/{uuid}_{timestamp}_{originalFilename}</li>
 *   <li>예: sightings/2025/01/original/abc123_20250101120000_cat.jpg</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Config s3Config;

    /**
     * 이미지 업로드 결과를 담는 내부 클래스
     */
    @lombok.Value(staticConstructor = "of")
    public static class ImageUploadResult {
        String originalUrl;     // 원본 이미지 URL (EXIF 포함)
        String sanitizedUrl;    // 공개 이미지 URL (EXIF 제거)
    }

    /**
     * 원본 이미지와 EXIF 제거 이미지를 S3에 업로드합니다.
     *
     * <p><b>업로드 프로세스:</b></p>
     * <ol>
     *   <li>원본 이미지 업로드 (EXIF 포함) → original/ 폴더</li>
     *   <li>EXIF 제거 이미지 업로드 → sanitized/ 폴더 (다른 파일명으로 보안 강화)</li>
     *   <li>두 URL 반환</li>
     * </ol>
     *
     * <p><b>보안 강화:</b></p>
     * <ul>
     *   <li>원본 파일명: uuid1_timestamp_original.jpg</li>
     *   <li>공개 파일명: uuid2_timestamp_sanitized.jpg (다른 UUID 사용)</li>
     *   <li>공개 URL로 원본 URL 추측 불가능</li>
     * </ul>
     *
     * @param originalFile 원본 이미지 파일 (EXIF 포함)
     * @param sanitizedBytes EXIF 제거된 이미지 바이트 배열
     * @return ImageUploadResult (원본 URL, 공개 URL)
     * @throws FileException 파일 업로드 실패 시
     */
    public ImageUploadResult uploadBothVersions(MultipartFile originalFile, byte[] sanitizedBytes) {
        try {
            // 1. 원본 파일명 생성
            String originalFileName = generateUniqueFileName(originalFile.getOriginalFilename());

            // 2. 공개 파일명 생성 (다른 UUID 사용으로 원본과 연관성 제거)
            String sanitizedFileName = generateUniqueFileName(originalFile.getOriginalFilename());

            // 3. 원본 이미지 업로드 (EXIF 포함)
            String originalS3Key = buildS3Key(originalFileName, "original");
            uploadToS3(originalS3Key, originalFile.getBytes(), originalFile.getContentType());
            String originalUrl = buildPublicUrl(originalS3Key);
            log.info("Original image uploaded to S3: {}", originalUrl);

            // 4. 공개 이미지 업로드 (EXIF 제거, 다른 파일명)
            String sanitizedS3Key = buildS3Key(sanitizedFileName, "sanitized");
            uploadToS3(sanitizedS3Key, sanitizedBytes, originalFile.getContentType());
            String sanitizedUrl = buildPublicUrl(sanitizedS3Key);
            log.info("Sanitized image uploaded to S3: {} (different filename for security)", sanitizedUrl);

            return ImageUploadResult.of(originalUrl, sanitizedUrl);

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new FileException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에 파일 업로드 (내부 메서드)
     *
     * @param s3Key S3 key
     * @param fileBytes 파일 바이트 배열
     * @param contentType 콘텐츠 타입
     */
    private void uploadToS3(String s3Key, byte[] fileBytes, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Config.getBucketName())
            .key(s3Key)
            .contentType(contentType)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
    }

    /**
     * S3에서 파일을 삭제합니다.
     *
     * @param fileUrl S3 파일 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // URL에서 S3 key 추출
            String s3Key = extractS3KeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(s3Key)
                .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: {}", fileUrl);

        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage(), e);
            throw new FileException(FileErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * 고유한 파일명 생성
     * 형식: {uuid}_{timestamp}_{originalFilename}
     *
     * @param originalFilename 원본 파일명
     * @return 고유한 파일명
     */
    private String generateUniqueFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("%s_%s_%s", uuid, timestamp, originalFilename);
    }

    /**
     * S3 key 생성
     * 형식: sightings/{year}/{month}/{type}/{filename}
     *
     * @param fileName 파일명
     * @param type 이미지 타입 (original 또는 sanitized)
     * @return S3 key
     */
    private String buildS3Key(String fileName, String type) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        return String.format("sightings/%s/%s/%s/%s", year, month, type, fileName);
    }

    /**
     * S3 공개 URL 생성
     *
     * @param s3Key S3 key
     * @return 공개 URL
     */
    private String buildPublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
            s3Config.getBucketName(),
            s3Config.getRegion(),
            s3Key);
    }

    /**
     * URL에서 S3 key 추출
     *
     * @param fileUrl S3 파일 URL
     * @return S3 key
     */
    private String extractS3KeyFromUrl(String fileUrl) {
        // https://bucket-name.s3.region.amazonaws.com/key 형식에서 key 추출
        String[] parts = fileUrl.split(".amazonaws.com/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid S3 URL format: " + fileUrl);
        }
        return parts[1];
    }
}
