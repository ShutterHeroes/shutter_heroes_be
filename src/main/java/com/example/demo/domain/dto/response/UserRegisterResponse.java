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
public class UserRegisterResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
    private LocalDateTime createdAt;

    public static UserRegisterResponse from(User user) {
        return UserRegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}