package com.example.demo.config.security;

import com.example.demo.exceptions.dto.CustomExceptionResponse;
import com.example.demo.exceptions.dto.WarnLogData;
import com.example.demo.exceptions.errorcode.AuthErrorCode;
import com.example.demo.exceptions.exception.AuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        CustomExceptionResponse errorResponse = CustomExceptionResponse.of(AuthErrorCode.UNAUTHORIZED);

        log.warn("SECURITY_UNAUTHORIZED_EXCEPTION_HANDLE",
            StructuredArguments.keyValue("exception", new WarnLogData(errorResponse, new AuthException(AuthErrorCode.UNAUTHORIZED)))
        );

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }
}
