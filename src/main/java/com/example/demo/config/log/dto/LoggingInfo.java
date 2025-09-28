package com.example.demo.config.log.dto;

import com.example.demo.config.log.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;

public record LoggingInfo(
    RequestInfo info
) {
    /**
     * 기본 요청 정보를 담는 로그 정보
     *
     * @param request        요청 데이터
     * @param cachedRequest  캐시된 요청 데이터
     */
    public LoggingInfo(HttpServletRequest request, CachedBodyHttpServletRequest cachedRequest) {
        this(new RequestInfo(request, cachedRequest));
    }
}
