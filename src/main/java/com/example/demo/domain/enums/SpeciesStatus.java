package com.example.demo.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SpeciesStatus {
    GENERAL("general"),
    ENDANGERED("endangered"),
    NATURAL_MONUMENT("natural_monument");


    private final String value;

    SpeciesStatus(String value) {
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
