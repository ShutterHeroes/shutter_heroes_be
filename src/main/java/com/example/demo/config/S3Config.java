package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 설정 클래스
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>S3Client Bean 생성</li>
 *   <li>AWS credentials 설정</li>
 *   <li>S3 bucket 정보 관리</li>
 * </ul>
 *
 * <p><b>설정 정보:</b></p>
 * <ul>
 *   <li>region: ap-northeast-2 (서울)</li>
 *   <li>bucket-name: shutter-heroes-bucket</li>
 *   <li>access-key, secret-key: application-secret.properties</li>
 * </ul>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class S3Config {

    /**
     * AWS S3 region (예: ap-northeast-2)
     */
    private String region;

    /**
     * S3 bucket 이름
     */
    private String bucketName;

    /**
     * AWS access key (application-secret.properties에서 주입)
     */
    private String accessKey;

    /**
     * AWS secret key (application-secret.properties에서 주입)
     */
    private String secretKey;

    /**
     * S3Client Bean 생성
     *
     * <p><b>설정 내용:</b></p>
     * <ul>
     *   <li>StaticCredentialsProvider로 access key, secret key 설정</li>
     *   <li>Region 설정 (서울 리전)</li>
     * </ul>
     *
     * @return S3Client AWS S3 클라이언트
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            .build();
    }
}
