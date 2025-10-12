package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON 직렬화/역직렬화 설정
 *
 * <p>LocalDateTime을 ISO-8601 UTC 형식으로 직렬화</p>
 * <ul>
 *   <li>출력 형식: "2025-10-09T15:16:00Z" (UTC 표시 포함)</li>
 *   <li>데이터베이스: UTC로 저장</li>
 *   <li>프론트엔드: UTC로 전송, 프론트엔드에서 로컬 시간대로 변환</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /**
     * ISO-8601 UTC 형식 (Z 포함)
     */
    private static final DateTimeFormatter ISO_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // JavaTimeModule 생성 및 커스터마이징
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime을 UTC로 직렬화 (Z 포함)
        javaTimeModule.addSerializer(java.time.LocalDateTime.class,
            new LocalDateTimeSerializer(ISO_WITH_ZONE));

        return builder
            .modules(javaTimeModule)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISO-8601 문자열 형식 사용
            .build();
    }
}
