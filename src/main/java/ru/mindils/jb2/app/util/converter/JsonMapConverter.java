package ru.mindils.jb2.app.util.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

@Converter(autoApply = false)
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, Object> {

    private static final Logger log = LoggerFactory.getLogger(JsonMapConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_OBJECT_TYPE_REFERENCE = new TypeReference<>() {};

    @Override
    public Object convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(attribute);
            PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            pGobject.setValue(jsonValue);
            return pGobject;
        } catch (JsonProcessingException | SQLException e) {
            log.error("Error converting Map<String, Object> to JSONB PGobject", e);
            throw new RuntimeException("Could not convert Map<String, Object> to JSONB", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return Collections.emptyMap();
        }

        if (dbData instanceof PGobject) {
            PGobject pgObject = (PGobject) dbData;
            if (!"jsonb".equals(pgObject.getType())) {
                log.error("PGobject type is not jsonb: {}", pgObject.getType());
                throw new RuntimeException("Invalid PGobject type: " + pgObject.getType());
            }

            String value = pgObject.getValue();
            if (value == null || value.isEmpty()) {
                return Collections.emptyMap();
            }

            try {
                return objectMapper.readValue(value, MAP_OBJECT_TYPE_REFERENCE);
            } catch (IOException e) {
                log.error("Error converting JSONB PGobject to Map<String, Object>", e);
                throw new RuntimeException("Error parsing JSONB value", e);
            }
        } else if (dbData instanceof String) {
            String raw = (String) dbData;
            if (raw.isEmpty()) {
                return Collections.emptyMap();
            }
            try {
                return objectMapper.readValue(raw, MAP_OBJECT_TYPE_REFERENCE);
            } catch (IOException e) {
                log.error("Error converting String DB data to Map<String, Object>", e);
                throw new RuntimeException("Error parsing JSONB string value", e);
            }
        } else {
            log.error("Unexpected object type received from database for JSONB mapping: {}", dbData.getClass().getName());
            throw new RuntimeException("Unexpected type for JSONB data: " + dbData.getClass().getName());
        }
    }
}