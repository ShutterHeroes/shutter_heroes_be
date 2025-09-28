package com.example.demo.config.log.dto;

import com.example.demo.config.log.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BodyInfo(
    @JsonProperty("request_body")
    String requestBody
) {
    public BodyInfo(CachedBodyHttpServletRequest wrappedRequest) {
        this(
            getBody(wrappedRequest)
        );
    }

    private static String getBody(CachedBodyHttpServletRequest wrappedRequest) {
        return wrappedRequest.readBody();
    }
}
