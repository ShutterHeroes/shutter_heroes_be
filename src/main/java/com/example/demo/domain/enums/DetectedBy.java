package com.example.demo.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DetectedBy {
    USER("user"),
    AI("ai");

    private final String value;

    DetectedBy(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
