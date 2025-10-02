package com.example.demo.exceptions.exception;

import com.example.demo.exceptions.errorcode.ErrorCode;

/**
 * 파일 관련 예외 클래스
 */
public class FileException extends CommonException {

    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
