package com.example.demo.domain.enums;

public enum JwtType {
    REFRESH_TOKEN,  // TODO 1: Refresh Token 추가 예정
    ACCESS_TOKEN;

    public boolean isAccessToken() {
        return this.equals(ACCESS_TOKEN);
    }
}
