package ru.mindils.jb2.app.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.dto.EmployerDto;
import ru.mindils.jb2.app.entity.Employer;

import java.util.List;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class EmployerMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    // Основные методы маппинга
    @Mapping(target = "logoUrls", source = "logoUrls", qualifiedByName = "mapStringToJsonNode")
    @Mapping(target = "relations", source = "relations", qualifiedByName = "listMapToJsonNode")
    @Mapping(target = "area", source = "area", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "industries", source = "industries", qualifiedByName = "listMapToJsonNode")
    @Mapping(target = "branding", source = "branding", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "insiderInterviews", source = "insiderInterviews", qualifiedByName = "listMapToJsonNode")
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "employerInfo", ignore = true)
    public abstract Employer toEntity(EmployerDto dto);

    @Mapping(target = "logoUrls", source = "logoUrls", qualifiedByName = "jsonNodeToMapString")
    @Mapping(target = "relations", source = "relations", qualifiedByName = "jsonNodeToListMapObject")
    @Mapping(target = "area", source = "area", qualifiedByName = "jsonNodeToMap")
    @Mapping(target = "industries", source = "industries", qualifiedByName = "jsonNodeToListMapObject")
    @Mapping(target = "branding", source = "branding", qualifiedByName = "jsonNodeToMap")
    @Mapping(target = "insiderInterviews", source = "insiderInterviews", qualifiedByName = "jsonNodeToListMapObject")
    public abstract EmployerDto toDto(Employer entity);

    // Методы конвертации JSON для Map<String, Object>
    @Named("mapToJsonNode")
    protected JsonNode mapToJsonNode(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return objectMapper.valueToTree(map);
    }

    @Named("jsonNodeToMap")
    @SuppressWarnings("unchecked")
    protected Map<String, Object> jsonNodeToMap(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        return objectMapper.convertValue(jsonNode, Map.class);
    }

    // Методы конвертации JSON для Map<String, String> (для logoUrls)
    @Named("mapStringToJsonNode")
    protected JsonNode mapStringToJsonNode(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        return objectMapper.valueToTree(map);
    }

    @Named("jsonNodeToMapString")
    @SuppressWarnings("unchecked")
    protected Map<String, String> jsonNodeToMapString(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        return objectMapper.convertValue(jsonNode, Map.class);
    }

    // Методы для работы со списками Map<String, Object>
    @Named("listMapToJsonNode")
    protected JsonNode listMapToJsonNode(List<Map<String, Object>> listMap) {
        if (listMap == null) {
            return null;
        }
        return objectMapper.valueToTree(listMap);
    }

    @Named("jsonNodeToListMapObject")
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> jsonNodeToListMapObject(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        return objectMapper.convertValue(jsonNode, List.class);
    }
}