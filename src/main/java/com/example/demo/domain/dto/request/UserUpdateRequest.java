package com.example.demo.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 수정 요청")
public class UserUpdateRequest {

    @Schema(description = "표시 이름", example = "홍길동")
    @Size(min = 1, max = 100, message = "표시 이름은 1자 이상 100자 이하여야 합니다")
    private String displayName;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다")
    private String avatarUrl;
}