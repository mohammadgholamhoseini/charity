package com.charity.app.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * Stores a free-form map in an existing {@code TEXT} column.
 *
 * <p>Replaces the previous hand-rolled pattern of a {@code detailsJson} string field plus a
 * {@code @Transient} map plus lazy getters that deserialised on first access. That version swallowed
 * parse failures into an empty map, so a single corrupt row silently lost its data on the next save.
 * Here a failure is logged and, on write, propagated -- losing data quietly is worse than failing.
 *
 * <p>A native MySQL {@code JSON} column would be nicer, but {@code ALTER TABLE ... MODIFY ... JSON}
 * fails outright if any existing row holds invalid JSON, which is not a risk worth taking against
 * the production table.
 */
@Slf4j
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("امکان ذخیره اطلاعات تکمیلی وجود ندارد", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            log.warn("Unreadable details JSON, treating as empty: {}", dbData, e);
            return Collections.emptyMap();
        }
    }
}
