package com.example.demo.domain.converter;

import com.example.demo.domain.enums.DetectedBy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DetectedByConverter implements AttributeConverter<DetectedBy, String> {

    @Override
    public String convertToDatabaseColumn(DetectedBy attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public DetectedBy convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (DetectedBy detectedBy : DetectedBy.values()) {
            if (detectedBy.getValue().equals(dbData)) {
                return detectedBy;
            }
        }

        throw new IllegalArgumentException("Unknown detected_by value: " + dbData);
    }
}
