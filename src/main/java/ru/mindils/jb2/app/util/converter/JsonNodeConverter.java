package ru.mindils.jb2.app.util.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

@Converter(autoApply = false)
public class JsonNodeConverter implements AttributeConverter<JsonNode, Object> {

    private static final Logger log = LoggerFactory.getLogger(JsonNodeConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null || attribute.isNull()) {
            return null;
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(attribute);
            PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            pGobject.setValue(jsonValue);
            return pGobject;
        } catch (JsonProcessingException | SQLException e) {
            log.error("Error converting JsonNode to JSONB PGobject", e);
            throw new RuntimeException("Could not convert JsonNode to JSONB", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }

        if (dbData instanceof PGobject) {
            PGobject pgObject = (PGobject) dbData;
            if (!"jsonb".equals(pgObject.getType())) {
                log.error("PGobject type is not jsonb: {}", pgObject.getType());
                throw new RuntimeException("Invalid PGobject type: " + pgObject.getType());
            }

            String value = pgObject.getValue();
            if (value == null || value.isEmpty()) {
                return null;
            }

            try {
                return objectMapper.readTree(value);
            } catch (IOException e) {
                log.error("Error converting JSONB PGobject to JsonNode", e);
                throw new RuntimeException("Error parsing JSONB value", e);
            }
        } else if (dbData instanceof String) {
            String raw = (String) dbData;
            if (raw.isEmpty()) {
                return null;
            }
            try {
                return objectMapper.readTree(raw);
            } catch (IOException e) {
                log.error("Error converting String DB data to JsonNode", e);
                throw new RuntimeException("Error parsing JSONB string value", e);
            }
        } else {
            log.error("Unexpected object type received from database for JSONB mapping: {}", dbData.getClass().getName());
            throw new RuntimeException("Unexpected type for JSONB data: " + dbData.getClass().getName());
        }
    }
}