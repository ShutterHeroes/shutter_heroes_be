package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;

public class SightingException extends CommonException {
    public SightingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
