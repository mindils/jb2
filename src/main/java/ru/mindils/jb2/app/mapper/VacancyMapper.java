package ru.mindils.jb2.app.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.dto.VacancyDto;
import ru.mindils.jb2.app.entity.Vacancy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class VacancyMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    // Единственный метод маппинга - DTO -> Entity
    @Mapping(target = "publishedAt", source = "publishedAt", qualifiedByName = "offsetToLocal")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetToLocal")
    @Mapping(target = "initialCreatedAt", source = "initialCreatedAt", qualifiedByName = "offsetToLocal")
    @Mapping(target = "billingType", source = "billingType", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "area", source = "area", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "salary", source = "salary", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "address", source = "address", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "experience", source = "experience", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "schedule", source = "schedule", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "employment", source = "employment", qualifiedByName = "mapToJsonNode")
    @Mapping(target = "keySkills", source = "keySkills", qualifiedByName = "listMapStringToJsonNode")
    @Mapping(target = "professionalRoles", source = "professionalRoles", qualifiedByName = "listMapStringToJsonNode")
    @Mapping(target = "workingTimeModes", source = "workingTimeModes", qualifiedByName = "listMapStringToJsonNode")
    @Mapping(target = "workFormat", source = "workFormat", qualifiedByName = "listMapStringToJsonNode")
    @Mapping(target = "employer", ignore = true) // будете устанавливать вручную
    @Mapping(target = "updatedAt", ignore = true) // это поле есть только в Entity
    public abstract Vacancy toEntity(VacancyDto dto);

    // Методы конвертации времени
    @Named("offsetToLocal")
    protected LocalDateTime offsetToLocal(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDateTime() : null;
    }

    // Методы конвертации JSON для Map<String, Object>
    @Named("mapToJsonNode")
    protected JsonNode mapToJsonNode(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return objectMapper.valueToTree(map);
    }

    // Методы для работы со списками Map<String, String> (для keySkills, professionalRoles и т.д.)
    @Named("listMapStringToJsonNode")
    protected JsonNode listMapStringToJsonNode(List<Map<String, String>> listMap) {
        if (listMap == null) {
            return null;
        }
        return objectMapper.valueToTree(listMap);
    }
}