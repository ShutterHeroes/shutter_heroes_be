package com.example.demo.domain.dto.response;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    /**
     * User 엔티티로부터 UserProfileResponse 생성
     *
     * @param user User 엔티티
     * @return UserProfileResponse
     */
    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .avatarUrl(user.getAvatarUrl())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}