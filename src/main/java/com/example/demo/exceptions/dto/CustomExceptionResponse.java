package com.example.demo.exceptions.dto;

import com.example.demo.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record CustomExceptionResponse(
    String code,
    String errorMessage
) {
    /**
     * Custom하게 생성한 CommonException의 responseDto를 반환한다.
     *
     * @param errorCode Custom하게 생성한 ErrorCode
     * @return CustomExceptionResponse
     */
    public static CustomExceptionResponse of(ErrorCode errorCode) {
        return new CustomExceptionResponse(
            errorCode.getCode(),
            errorCode.getMessage()
        );
    }

    /**
     * InternalServerError가 발생했을 경우, 정해진 responseDto를 반환한다.
     *
     * @param e InternalServerError라고 설정한 Exception
     * @return CustomExceptionResponse
     */
    public static CustomExceptionResponse ofInternalServerError(Exception e) {
        return new CustomExceptionResponse(
            "INTERNAL_SERVER_ERROR",
            e.getMessage()
        );
    }

    /**
     * Custom하게 생성한 CommonException의 ResponseEntity를 반환한다.
     *
     * @param errorCode Custom하게 생성한 ErrorCode
     * @return ResponseEntity
     */
    public static ResponseEntity<CustomExceptionResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus().value())
            .body(new CustomExceptionResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 상태, 코드 등을 지정하여 생성한 ResponseEntity를 반환한다.
     *
     * @param httpStatus HttpStatus
     * @param code       에러 코드
     * @param e          Exception
     * @return ResponseEntity
     */
    public static ResponseEntity<CustomExceptionResponse> toResponseEntity(HttpStatus httpStatus, String code, Exception e) {
        return ResponseEntity.status(httpStatus.value())
            .body(new CustomExceptionResponse(code, e.getMessage()));
    }
}
