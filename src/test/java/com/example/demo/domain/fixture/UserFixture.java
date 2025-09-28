package com.example.demo.domain.fixture;

import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.response.UserRegisterResponse;
import com.example.demo.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User 관련 테스트 데이터 생성을 위한 Fixture 클래스
 */
public class UserFixture {

    // 기본값 상수
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_PASSWORD = "password123";
    private static final String DEFAULT_DISPLAY_NAME = "테스트유저";
    private static final String DEFAULT_AVATAR_URL = "https://example.com/avatar.jpg";

    /**
     * 기본 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createDefaultRegisterRequest() {
        return createUserRegisterRequest(DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME, DEFAULT_AVATAR_URL);
    }

    /**
     * 필수 필드만 포함한 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createMinimalRegisterRequest() {
        return createUserRegisterRequest(DEFAULT_EMAIL, DEFAULT_PASSWORD, null, null);
    }

    /**
     * 이메일이 누락된 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithoutEmail() {
        return createUserRegisterRequest(null, DEFAULT_PASSWORD, null, null);
    }

    /**
     * 비밀번호가 누락된 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithoutPassword() {
        return createUserRegisterRequest(DEFAULT_EMAIL, null, null, null);
    }

    /**
     * 잘못된 이메일 형식의 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithInvalidEmail() {
        return createUserRegisterRequest("invalid-email", DEFAULT_PASSWORD, null, null);
    }

    /**
     * 짧은 비밀번호의 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithShortPassword() {
        return createUserRegisterRequest(DEFAULT_EMAIL, "short", null, null);
    }

    /**
     * 중복된 이메일의 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithDuplicateEmail() {
        return createUserRegisterRequest("duplicate@example.com", DEFAULT_PASSWORD, null, null);
    }

    /**
     * 빈 이메일의 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithEmptyEmail() {
        return createUserRegisterRequest("", DEFAULT_PASSWORD, null, null);
    }

    /**
     * 빈 비밀번호의 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRequestWithEmptyPassword() {
        return createUserRegisterRequest(DEFAULT_EMAIL, "", null, null);
    }

    /**
     * 커스텀 이메일로 UserRegisterRequest 생성
     */
    public static UserRegisterRequest createRegisterRequest(String email) {
        return createUserRegisterRequest(email, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME, DEFAULT_AVATAR_URL);
    }

    /**
     * 기본 UserRegisterResponse 생성 (모든 필드 포함)
     */
    public static UserRegisterResponse createDefaultRegisterResponse() {
        return createUserRegisterResponse(DEFAULT_EMAIL, DEFAULT_DISPLAY_NAME, DEFAULT_AVATAR_URL);
    }

    /**
     * 필수 필드만 포함한 UserRegisterResponse 생성
     */
    public static UserRegisterResponse createMinimalRegisterResponse() {
        return createUserRegisterResponse(DEFAULT_EMAIL, "test", null);  // email의 @ 앞부분
    }

    /**
     * 커스텀 이메일로 UserRegisterResponse 생성
     */
    public static UserRegisterResponse createRegisterResponse(String email, String displayName) {
        return createUserRegisterResponse(email, displayName, DEFAULT_AVATAR_URL);
    }

    /**
     * UserRegisterRequest 생성 private 메서드
     */
    private static UserRegisterRequest createUserRegisterRequest(String email, String password, String displayName, String avatarUrl) {
        UserRegisterRequest.UserRegisterRequestBuilder builder = UserRegisterRequest.builder();

        if (email != null) {
            builder.email(email);
        }
        if (password != null) {
            builder.password(password);
        }
        if (displayName != null) {
            builder.displayName(displayName);
        }
        if (avatarUrl != null) {
            builder.avatarUrl(avatarUrl);
        }

        return builder.build();
    }

    /**
     * UserRegisterResponse 생성 private 메서드
     */
    private static UserRegisterResponse createUserRegisterResponse(String email, String displayName, String avatarUrl) {
        UserRegisterResponse.UserRegisterResponseBuilder builder = UserRegisterResponse.builder()
            .id(UUID.randomUUID())
            .role(UserRole.USER)
            .createdAt(LocalDateTime.now());

        if (email != null) {
            builder.email(email);
        }
        if (displayName != null) {
            builder.displayName(displayName);
        }
        if (avatarUrl != null) {
            builder.avatarUrl(avatarUrl);
        }

        return builder.build();
    }
}
