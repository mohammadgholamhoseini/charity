package com.charity.app.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a list of uploaded document filenames out of a {@code TEXT} column.
 * See {@link JsonMapConverter} for why this is a converter over TEXT rather than a native JSON column.
 *
 * <p>No entity maps a field through it any more: documents are rows in {@code request_documents}.
 * It survives as the parser for {@code RequestDocumentBackfill}, which has to read exactly what the
 * old mapping wrote -- including the same tolerance for an unreadable value, which for a backfill
 * running on every startup is the difference between skipping one row and refusing to boot.
 */
@Slf4j
@Converter
public class JsonListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("امکان ذخیره فهرست مدارک وجود ندارد", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            log.warn("Unreadable documents JSON, treating as empty: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
