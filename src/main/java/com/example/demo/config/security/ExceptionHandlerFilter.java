package com.example.demo.config.security;

import com.example.demo.exceptions.CommonException;
import com.example.demo.exceptions.dto.CustomExceptionResponse;
import com.example.demo.exceptions.dto.WarnLogData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CommonException e) {
            handleCommonException(response, e);
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    private void handleCommonException(HttpServletResponse response, CommonException e) throws IOException {
        response.setStatus(e.getErrorCode().getHttpStatus().value());
        response.setContentType("application/json; charset=UTF-8");
        CustomExceptionResponse errorResponse = CustomExceptionResponse.of(e.getErrorCode());

        log.warn("FILTER_COMMON_EXCEPTION_HANDLE",
            StructuredArguments.keyValue("exception", new WarnLogData(errorResponse, e))
        );

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

    private void handleException(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType("application/json; charset=UTF-8");
        CustomExceptionResponse errorResponse = CustomExceptionResponse.ofInternalServerError(e);

        log.error("FILTER_EXCEPTION_HANDLE",
            StructuredArguments.keyValue("exception", new WarnLogData(errorResponse, e))
        );

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }
}
