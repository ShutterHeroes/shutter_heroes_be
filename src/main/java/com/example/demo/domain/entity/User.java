package com.example.demo.domain.entity;

import com.example.demo.domain.converter.UserRoleConverter;
import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.request.UserUpdateRequest;
import com.example.demo.domain.enums.LoginPlatform;
import com.example.demo.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false, name = "role")
    @ColumnTransformer(
        read = "role::varchar",
        write = "?::app.role"
    )
    @Convert(converter = UserRoleConverter.class)
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 신규 유저 회원가입 시 User를 생성하여 반환합니다.
     *
     * @param email         요청 이메일
     * @param displayName   요청 이름
     * @param loginPlatform 요청 로그인 플랫폼
     * @return 생성된 User
     */
    public static User ofNewRegistration(String email, String displayName, LoginPlatform loginPlatform) {
        // 필요할 경우, loginPlatform에 따른 추가 로직 구현
        return User.builder()
            .email(email)
            .passwordHash(UUID.randomUUID().toString())
            .displayName(displayName)
            .role(UserRole.USER)
            .build();
    }

    /**
     * 회원가입 요청을 User 엔티티로 변환
     *
     * @param passwordEncoder 비밀번호 인코더
     * @param request 회원가입 요청 DTO
     * @return User 엔티티
     */
    public static User toEntity(PasswordEncoder passwordEncoder, UserRegisterRequest request) {
        // displayName이 없으면 이메일의 @ 앞부분을 사용
        String displayName = StringUtils.hasText(request.getDisplayName())
            ? request.getDisplayName()
            : request.getEmail().split("@")[0];

        // 사용자 엔티티 생성
        return User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .displayName(displayName)
            .avatarUrl(request.getAvatarUrl())
            .role(UserRole.USER)
            .build();
    }

    /**
     * 사용자 정보 업데이트
     *
     * @param request 업데이트 요청 DTO
     */
    public void update(UserUpdateRequest request) {
        // displayName 업데이트
        if (StringUtils.hasText(request.getDisplayName())) {
            this.displayName = request.getDisplayName();
        }

        // avatarUrl 업데이트
        if (request.getAvatarUrl() != null) {
            this.avatarUrl = request.getAvatarUrl();
        }

        // updatedAt 업데이트
        this.updatedAt = LocalDateTime.now();
    }
}
