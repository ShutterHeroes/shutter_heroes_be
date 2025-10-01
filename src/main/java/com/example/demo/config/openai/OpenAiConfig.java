package com.example.demo.config.openai;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {

    private String apiKey;
    private String apiUrl;
    private String model;           // 텍스트 전용 모델 (gpt-4.1-mini)
    private String visionModel;     // Vision API 전용 모델 (gpt-4o)

    @Bean
    public WebClient openAiWebClient() {
        // HttpClient에서 타임아웃 설정 - 모든 타임아웃을 120초로 명시적 설정
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000) // 연결 타임아웃 120초
            .responseTimeout(Duration.ofSeconds(120)) // 응답 타임아웃 120초
            .doOnConnected(conn -> {
                log.info("Setting up connection handlers with 120s timeout");
                conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS));
            })
            .keepAlive(true) // keep-alive 활성화
            .wiretap(true); // 디버깅을 위한 wire tap 활성화

        return WebClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
