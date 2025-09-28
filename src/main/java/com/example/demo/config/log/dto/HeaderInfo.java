package com.example.demo.config.log.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public record HeaderInfo(
    String cookie,
    @JsonProperty("content_type")
    String contentType,
    String authorization
) {
    public HeaderInfo(HttpServletRequest request) {
        this(
            getCookie(request),
            request.getHeader(HttpHeaders.CONTENT_TYPE),
            getAuthorization(request)
        );
    }

    private static String getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (isNull(cookies)) {
            return null;
        }
        return Arrays.stream(cookies)
            .map(c -> String.format("%s:%s", c.getName(), c.getValue()))
            .collect(Collectors.joining(", "));
    }

    private static String getAuthorization(HttpServletRequest request) {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isNull(authorization)) {
            return null;
        }
        return authorization;
    }
}
