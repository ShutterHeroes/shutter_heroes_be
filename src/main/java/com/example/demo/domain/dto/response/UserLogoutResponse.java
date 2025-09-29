package com.example.demo.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLogoutResponse {

    private String message;
    private boolean success;

    public static UserLogoutResponse success() {
        return UserLogoutResponse.builder()
            .message("Successfully logged out")
            .success(true)
            .build();
    }
}