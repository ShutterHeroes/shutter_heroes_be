package com.example.demo.exceptions.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 파일 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    // 파일 업로드 관련 에러 코드
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_0001", "파일 업로드에 실패했습니다"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_0002", "파일 삭제에 실패했습니다"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE_0003", "지원하지 않는 파일 형식입니다"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE_0004", "파일 크기가 너무 큽니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_0005", "파일을 찾을 수 없습니다"),

    // EXIF 관련 에러 코드
    EXIF_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_0006", "EXIF 메타데이터 추출에 실패했습니다"),
    INVALID_GPS_DATA(HttpStatus.BAD_REQUEST, "FILE_0007", "유효하지 않은 GPS 데이터입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
