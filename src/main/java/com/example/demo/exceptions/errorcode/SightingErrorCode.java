package com.example.demo.exceptions.errorcode;

import org.springframework.http.HttpStatus;

public enum SightingErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "S_0001", "존재하지 않는 Sighting입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "S_0002", "해당 Sighting에 대한 권한이 없습니다"),
    INVALID_VISIBILITY(HttpStatus.BAD_REQUEST, "S_0003", "유효하지 않은 공개 설정입니다. PUBLIC 또는 PRIVATE만 허용됩니다"),
    NO_ANIMAL_DETECTED(HttpStatus.BAD_REQUEST, "S_0004", "이미지에서 동물이 감지되지 않았습니다");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String errorMessage;

    SightingErrorCode(HttpStatus httpStatus, String errorCode, String errorMessage) {
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
