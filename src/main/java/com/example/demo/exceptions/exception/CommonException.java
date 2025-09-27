package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;
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
