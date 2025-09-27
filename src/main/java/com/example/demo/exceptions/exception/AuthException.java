package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;

public class AuthException extends CommonException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
