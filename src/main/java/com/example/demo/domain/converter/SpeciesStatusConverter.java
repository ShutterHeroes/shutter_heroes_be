package com.example.demo.domain.converter;

import com.example.demo.domain.enums.SpeciesStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpeciesStatusConverter implements AttributeConverter<SpeciesStatus, String> {

    @Override
    public String convertToDatabaseColumn(SpeciesStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public SpeciesStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (SpeciesStatus status : SpeciesStatus.values()) {
            if (status.getValue().equals(dbData)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown species status value: " + dbData);
    }
}