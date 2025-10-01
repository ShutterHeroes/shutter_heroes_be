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
 *   <li>이미지 파일 S3 업로드</li>
 *   <li>고유한 파일명 생성 (UUID + timestamp)</li>
 *   <li>S3 URL 반환</li>
 *   <li>파일 삭제</li>
 * </ul>
 *
 * <p><b>파일 경로 구조:</b></p>
 * <ul>
 *   <li>sightings/{year}/{month}/{uuid}_{timestamp}_{originalFilename}</li>
 *   <li>예: sightings/2025/01/abc123_20250101120000_cat.jpg</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Config s3Config;

    /**
     * 이미지 파일을 S3에 업로드하고 URL을 반환합니다.
     *
     * <p><b>업로드 프로세스:</b></p>
     * <ol>
     *   <li>고유한 파일명 생성 (UUID + timestamp)</li>
     *   <li>S3 경로 생성 (sightings/yyyy/MM/)</li>
     *   <li>S3에 파일 업로드</li>
     *   <li>공개 URL 반환</li>
     * </ol>
     *
     * @param file 업로드할 이미지 파일
     * @return S3 파일 URL
     * @throws FileException 파일 업로드 실패 시
     */
    public String uploadImage(MultipartFile file) {
        try {
            // 1. 고유한 파일명 생성
            String fileName = generateUniqueFileName(file.getOriginalFilename());

            // 2. S3 경로 생성 (sightings/yyyy/MM/)
            String s3Key = buildS3Key(fileName);

            // 3. S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // 4. 공개 URL 반환
            String fileUrl = buildPublicUrl(s3Key);
            log.info("File uploaded successfully to S3: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new FileException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
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
     * 형식: sightings/{year}/{month}/{filename}
     *
     * @param fileName 파일명
     * @return S3 key
     */
    private String buildS3Key(String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        return String.format("sightings/%s/%s/%s", year, month, fileName);
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
