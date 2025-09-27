package com.example.demo.domain.entity;

import com.example.demo.domain.converter.UserRoleConverter;
import com.example.demo.domain.enums.LoginPlatform;
import com.example.demo.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "app")
@Getter
@Setter
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
}
