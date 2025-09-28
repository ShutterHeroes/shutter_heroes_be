package com.example.demo.exceptions.errorcode;

import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {
    NOT_EXIST(HttpStatus.NOT_FOUND, "U_0001", "존재하지 않는 유저입니다"),
    DEACTIVATED_USER(HttpStatus.FORBIDDEN, "U_0002", "비활성화된 사용자입니다");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String errorMessage;

    UserErrorCode(HttpStatus httpStatus, String errorCode, String errorMessage) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }
}
