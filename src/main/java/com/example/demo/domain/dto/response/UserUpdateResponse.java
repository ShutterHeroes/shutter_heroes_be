package com.example.demo.domain.dto.response;

import com.example.demo.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "사용자 정보 수정 응답")
public class UserUpdateResponse {

    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "표시 이름", example = "홍길동")
    private String displayName;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "수정 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    public static UserUpdateResponse from(User user) {
        return UserUpdateResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}