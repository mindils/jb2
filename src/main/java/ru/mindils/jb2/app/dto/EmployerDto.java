package ru.mindils.jb2.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class EmployerDto {
    private String id;
    private String name;
    private Boolean trusted;

    @JsonProperty("accredited_it_employer")
    private Boolean accreditedItEmployer;

    @JsonProperty("has_divisions")
    private Boolean hasDivisions;

    private String type;
    private String description;

    @JsonProperty("site_url")
    private String siteUrl;

    @JsonProperty("alternate_url")
    private String alternateUrl;

    @JsonProperty("vacancies_url")
    private String vacanciesUrl;

    @JsonProperty("logo_urls")
    private Map<String, String> logoUrls;

    private List<Map<String, Object>> relations;
    private Map<String, Object> area;
    private List<Map<String, Object>> industries;

    @JsonProperty("branded_description")
    private String brandedDescription;

    private Map<String, Object> branding;

    @JsonProperty("insider_interviews")
    private List<Map<String, Object>> insiderInterviews;

    @JsonProperty("open_vacancies")
    private Integer openVacancies;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}