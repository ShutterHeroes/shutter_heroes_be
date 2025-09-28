package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;

public class UserException extends CommonException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
