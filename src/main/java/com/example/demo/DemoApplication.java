package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class DemoApplication {

	/**
	 * JVM 기본 시간대를 UTC로 설정
	 *
	 * 데이터베이스에는 UTC 기준으로 저장하고,
	 * 프론트엔드에서 사용자의 로컬 시간대(KST 등)로 변환하여 표시
	 */
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
