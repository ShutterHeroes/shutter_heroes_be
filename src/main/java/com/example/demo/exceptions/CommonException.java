package com.example.demo.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CommonException extends RuntimeException {

    private final ErrorCode errorCode;

    public String getCode() {
        return errorCode.getCode();
    }

    public String getMessage() {
        return errorCode.getMessage();
    }
}
