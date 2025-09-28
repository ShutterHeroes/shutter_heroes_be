package com.example.demo.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailExistsResponse {

    private boolean exists;

    public static UserEmailExistsResponse of(boolean exists) {
        return UserEmailExistsResponse.builder()
                .exists(exists)
                .build();
    }
}