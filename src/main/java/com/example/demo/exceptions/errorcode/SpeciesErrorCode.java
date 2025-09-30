package com.example.demo.exceptions.errorcode;

import org.springframework.http.HttpStatus;

public enum SpeciesErrorCode implements ErrorCode {
    NOT_EXIST(HttpStatus.NOT_FOUND, "SPECIES_0001", "존재하지 않는 종 정보입니다"),
    INVALID_SCIENTIFIC_NAME(HttpStatus.BAD_REQUEST, "SPECIES_0002", "올바르지 않은 학명 형식입니다"),
    FAILED_TO_PARSE_OPENAI_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "SPECIES_0003", "OpenAI 응답 파싱에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String errorMessage;

    SpeciesErrorCode(HttpStatus httpStatus, String errorCode, String errorMessage) {
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