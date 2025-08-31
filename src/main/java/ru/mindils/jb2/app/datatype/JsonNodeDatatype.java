package ru.mindils.jb2.app.datatype;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.metamodel.annotation.DatatypeDef;
import io.jmix.core.metamodel.annotation.Ddl;
import io.jmix.core.metamodel.datatype.Datatype;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Datatype for Jackson JsonNode that maps to PostgreSQL jsonb column.
 */
@Component
@DatatypeDef(
        id = "jsonNode",
        javaClass = JsonNode.class,
        defaultForClass = true
)
@Ddl("jsonb")
public class JsonNodeDatatype implements Datatype<JsonNode> {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String format(@Nullable Object value) {
    if (value == null) return null;
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new RuntimeException("Unable to serialize JsonNode", e);
    }
  }

  @Override
  public String format(@Nullable Object value, Locale locale) {
    return format(value);
  }

  @Override
  public JsonNode parse(String value) {
    try {
      return mapper.readTree(value);
    } catch (Exception e) {
      throw new RuntimeException("Unable to parse JsonNode", e);
    }
  }

  @Override
  public JsonNode parse(String value, Locale locale) {
    return parse(value);
  }
}