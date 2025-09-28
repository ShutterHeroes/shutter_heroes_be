package com.example.demo.domain.dto.response;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private UserRole role;

    public static UserLoginResponse from(User user) {
        return UserLoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}