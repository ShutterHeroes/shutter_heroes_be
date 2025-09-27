package com.example.demo.domain.converter;

import com.example.demo.domain.enums.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (UserRole role : UserRole.values()) {
            if (role.getValue().equals(dbData)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role value: " + dbData);
    }
}
