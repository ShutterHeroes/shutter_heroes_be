package com.example.demo.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Visibility {
    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    Visibility(String value) {
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
