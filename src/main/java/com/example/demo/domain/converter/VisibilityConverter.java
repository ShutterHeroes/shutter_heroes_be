package com.example.demo.domain.converter;

import com.example.demo.domain.enums.Visibility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class VisibilityConverter implements AttributeConverter<Visibility, String> {

    @Override
    public String convertToDatabaseColumn(Visibility attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public Visibility convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (Visibility visibility : Visibility.values()) {
            if (visibility.getValue().equals(dbData)) {
                return visibility;
            }
        }

        throw new IllegalArgumentException("Unknown visibility value: " + dbData);
    }
}
