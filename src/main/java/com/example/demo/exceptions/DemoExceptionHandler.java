package com.example.demo.exceptions;

import com.example.demo.exceptions.dto.CustomExceptionResponse;
import com.example.demo.exceptions.dto.WarnLogData;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class DemoExceptionHandler {

	@ExceptionHandler(CommonException.class)
	public ResponseEntity<CustomExceptionResponse> handleException(final CommonException e) {
		final ResponseEntity<CustomExceptionResponse> response = CustomExceptionResponse.toResponseEntity(e.getErrorCode());

		log.warn("CONTROLLER_COMMON_EXCEPTION_HANDLE",
			StructuredArguments.keyValue("exception", new WarnLogData(response.getBody(), e))
		);
		return response;
	}

	@ExceptionHandler({HttpClientErrorException.class})
	public ResponseEntity<CustomExceptionResponse> handleHttpClientErrorException(final HttpClientErrorException e) {
		final ResponseEntity<CustomExceptionResponse> response = CustomExceptionResponse.toResponseEntity(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"HTTP_CLIENT_ERROR_EXCEPTION",
			e
		);

		log.warn("CONTROLLER_HTTP_CLIENT_ERROR_EXCEPTION_HANDLE",
			StructuredArguments.keyValue("exception", new WarnLogData(response.getBody(), e))
		);
		return response;
	}

	@ExceptionHandler({DataIntegrityViolationException.class, MethodArgumentNotValidException.class, MissingServletRequestParameterException.class, ConstraintViolationException.class})
	public ResponseEntity<CustomExceptionResponse> handleBadRequestException(final Exception e) {
		final ResponseEntity<CustomExceptionResponse> response = CustomExceptionResponse.toResponseEntity(
			HttpStatus.BAD_REQUEST,
			"BAD_REQUEST_EXCEPTION",
			e
		);

		log.warn("CONTROLLER_BAD_REQUEST_EXCEPTION_HANDLE",
			StructuredArguments.keyValue("exception", new WarnLogData(response.getBody(), e))
		);
		return response;
	}
}
