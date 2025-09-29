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
public class UserPublicInfoResponse {

    private UUID id;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
    private LocalDateTime createdAt;

    /**
     * User 엔티티로부터 공개 정보만 포함한 UserPublicInfoResponse 생성
     * 이메일, 마지막 로그인 시간 등 민감한 정보는 제외
     *
     * @param user User 엔티티
     * @return UserPublicInfoResponse
     */
    public static UserPublicInfoResponse from(User user) {
        return UserPublicInfoResponse.builder()
            .id(user.getId())
            .displayName(user.getDisplayName())
            .avatarUrl(user.getAvatarUrl())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}