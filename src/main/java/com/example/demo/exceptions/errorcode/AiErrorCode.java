package com.example.demo.exceptions.errorcode;

import org.springframework.http.HttpStatus;

public enum AiErrorCode implements ErrorCode {
    OPENAI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_0001", "OpenAI API 호출 중 오류가 발생했습니다"),
    OPENAI_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "AI_0002", "OpenAI API 호출 시간이 초과되었습니다"),
    INVALID_SCIENTIFIC_NAME(HttpStatus.BAD_REQUEST, "AI_0003", "올바르지 않은 학명 형식입니다"),
    EMPTY_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AI_0004", "OpenAI API에서 빈 응답을 받았습니다"),
    OPENAI_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AI_0005", "OpenAI API 키가 유효하지 않습니다"),
    OPENAI_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI_0006", "OpenAI API 사용량 한도를 초과했습니다"),
    OPENAI_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "AI_0007", "OpenAI API 요청 속도 제한에 걸렸습니다");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String errorMessage;

    AiErrorCode(HttpStatus httpStatus, String errorCode, String errorMessage) {
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
