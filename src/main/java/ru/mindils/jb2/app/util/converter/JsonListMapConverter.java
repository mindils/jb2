package ru.mindils.jb2.app.util.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mindils.jb2.app.exception.InvalidJsonbException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Converter(autoApply = false)
public class JsonListMapConverter implements AttributeConverter<List<Map<String, Object>>, Object> {

    private static final Logger log = LoggerFactory.getLogger(JsonListMapConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE_REFERENCE = new TypeReference<>() {};

    @Override
    public Object convertToDatabaseColumn(List<Map<String, Object>> attribute) {
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
            log.error("Error converting List<Map<String, Object>> to JSONB PGobject", e);
            throw new InvalidJsonbException("Could not convert List<Map<String, Object>> to JSONB", e);
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return Collections.emptyList();
        }

        if (dbData instanceof PGobject) {
            PGobject pgObject = (PGobject) dbData;
            if (!"jsonb".equals(pgObject.getType())) {
                log.error("PGobject type is not jsonb: {}", pgObject.getType());
                throw new InvalidJsonbException("Invalid PGobject type: " + pgObject.getType());
            }

            String value = pgObject.getValue();
            if (value == null || value.isEmpty()) {
                return Collections.emptyList();
            }

            try {
                return objectMapper.readValue(value, LIST_MAP_TYPE_REFERENCE);
            } catch (IOException e) {
                log.error("Error converting JSONB PGobject to List<Map<String, Object>>", e);
                throw new InvalidJsonbException("Error parsing JSONB value", e);
            }
        } else if (dbData instanceof String) {
            String raw = (String) dbData;
            if (raw.isEmpty()) {
                return Collections.emptyList();
            }
            try {
                return objectMapper.readValue(raw, LIST_MAP_TYPE_REFERENCE);
            } catch (IOException e) {
                log.error("Error converting String DB data to List<Map<String, Object>>", e);
                throw new InvalidJsonbException("Error parsing JSONB string value", e);
            }
        } else {
            log.error("Unexpected object type received from database for JSONB mapping: {}", dbData.getClass().getName());
            throw new InvalidJsonbException("Unexpected type for JSONB data: " + dbData.getClass().getName());
        }
    }
}
