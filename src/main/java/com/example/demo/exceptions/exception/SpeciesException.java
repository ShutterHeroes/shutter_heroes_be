package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;

public class SpeciesException extends CommonException {
    public SpeciesException(ErrorCode errorCode) {
        super(errorCode);
    }
}