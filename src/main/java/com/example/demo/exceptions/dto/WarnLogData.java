package com.example.demo.exceptions.dto;

import java.util.Arrays;

public record WarnLogData(
	String errorCode,
	String errorMessage,
	String detailErrorMessage,
	String errorClass,
	String stackTrace
) {
	public WarnLogData(CustomExceptionResponse errorResponse, Exception e) {
		this(
			errorResponse.code(),
			errorResponse.errorMessage(),
			e.getMessage(),
			e.getClass().getSimpleName(),
			Arrays.toString(e.getStackTrace())
		);
	}

	public WarnLogData(CustomExceptionResponse errorResponse, Exception e, String message) {
		this(
			errorResponse.code(),
			errorResponse.errorMessage(),
			message,
			e.getClass().getSimpleName(),
			Arrays.toString(e.getStackTrace())
		);
	}

	public WarnLogData(String errorCode, String errorMessage, Exception e) {
		this(
			errorCode,
			errorMessage,
			e.getMessage(),
			e.getClass().getSimpleName(),
			Arrays.toString(e.getStackTrace())
		);
	}
}
