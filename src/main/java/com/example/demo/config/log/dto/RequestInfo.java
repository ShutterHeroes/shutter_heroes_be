package com.example.demo.config.log.dto;

import com.example.demo.config.log.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public record RequestInfo(
    String clientIp,
    String method,
    String url,
    HeaderInfo header,
    BodyInfo body,
    Map<String, String[]> queryParams
) {
    public RequestInfo(HttpServletRequest request, CachedBodyHttpServletRequest wrappedRequest) {
        this(
            getClientIp(request),
            request.getMethod(),
            request.getRequestURI(),
            new HeaderInfo(request),
            new BodyInfo(wrappedRequest),
            request.getParameterMap()
        );
    }

    private static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (!isEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
