package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리를 위한 설정 클래스
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>@Async 어노테이션 활성화</li>
 *   <li>Species 정보 비동기 처리를 위한 전용 ThreadPool 설정</li>
 *   <li>에러 핸들링 및 로깅</li>
 * </ul>
 *
 * <p><b>ThreadPool 설정:</b></p>
 * <ul>
 *   <li>Core Pool Size: 2 (기본 스레드 수)</li>
 *   <li>Max Pool Size: 5 (최대 스레드 수)</li>
 *   <li>Queue Capacity: 100 (대기 큐 크기)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Species 처리를 위한 비동기 Executor 생성
     *
     * <p><b>동작 방식:</b></p>
     * <ol>
     *   <li>Core Pool Size(2)만큼 스레드 생성</li>
     *   <li>모든 스레드가 사용 중이면 Queue(100)에 작업 대기</li>
     *   <li>Queue가 가득 차면 Max Pool Size(5)까지 스레드 추가 생성</li>
     *   <li>Max Pool Size 초과 시 RejectedExecutionException 발생</li>
     * </ol>
     *
     * @return ThreadPoolTaskExecutor 비동기 작업 실행을 위한 스레드풀
     */
    @Bean(name = "speciesTaskExecutor")
    public Executor speciesTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수 (항상 유지)
        executor.setCorePoolSize(2);

        // 최대 스레드 수 (부하 발생 시)
        executor.setMaxPoolSize(5);

        // 대기 큐 크기 (Core Pool이 가득 찰 때 대기)
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사 (디버깅 및 로깅용)
        executor.setThreadNamePrefix("species-async-");

        // 종료 시 모든 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 종료 대기 시간 (초)
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Species Task Executor initialized with corePoolSize=2, maxPoolSize=5, queueCapacity=100");

        return executor;
    }
}
