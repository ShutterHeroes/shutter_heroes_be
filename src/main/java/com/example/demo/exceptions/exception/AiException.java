package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;

public class AiException extends CommonException {
    public AiException(ErrorCode errorCode) {
        super(errorCode);
    }
}
