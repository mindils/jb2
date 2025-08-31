package ru.mindils.jb2.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class VacancyDto {
    private String id;
    private String name;
    private String description;

    @JsonProperty("branded_description")
    private String brandedDescription;

    private Boolean premium;

    @JsonProperty("billing_type")
    private Map<String, Object> billingType;

    private Map<String, Object> area;
    private Map<String, Object> salary;
    private Map<String, Object> address;
    private Map<String, Object> experience;
    private Map<String, Object> schedule;
    private Map<String, Object> employment;
    private EmployerDto employer;

    @JsonProperty("key_skills")
    private List<Map<String, String>> keySkills;

    @JsonProperty("professional_roles")
    private List<Map<String, String>> professionalRoles;

    @JsonProperty("working_time_modes")
    private List<Map<String, String>> workingTimeModes;

    @JsonProperty("work_format")
    private List<Map<String, String>> workFormat;

    @JsonProperty("response_letter_required")
    private Boolean responseLetterRequired;

    @JsonProperty("allow_messages")
    private Boolean allowMessages;

    @JsonProperty("has_test")
    private Boolean hasTest;

    private Boolean archived;
    private Boolean hidden;

    @JsonProperty("accept_handicapped")
    private Boolean acceptHandicapped;

    @JsonProperty("accept_kids")
    private Boolean acceptKids;

    @JsonProperty("accept_incomplete_resumes")
    private Boolean acceptIncompleteResumes;

    @JsonProperty("quick_responses_allowed")
    private Boolean quickResponsesAllowed;

    private Boolean approved;
    private Boolean internship;

    @JsonProperty("night_shifts")
    private Boolean nightShifts;

    @JsonProperty("accept_temporary")
    private Boolean acceptTemporary;

    @JsonProperty("show_logo_in_search")
    private Boolean showLogoInSearch;

    @JsonProperty("closed_for_applicants")
    private Boolean closedForApplicants;

    @JsonProperty("response_url")
    private String responseUrl;

    @JsonProperty("apply_alternate_url")
    private String applyAlternateUrl;

    @JsonProperty("alternate_url")
    private String alternateUrl;

    @JsonProperty("published_at")
    private OffsetDateTime publishedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("initial_created_at")
    private OffsetDateTime initialCreatedAt;

    @Data
    public static class EmployerDto {
        private String id;
        private String name;
        private String url;

        @JsonProperty("alternate_url")
        private String alternateUrl;

        @JsonProperty("vacancies_url")
        private String vacanciesUrl;

        @JsonProperty("logo_urls")
        private Map<String, String> logoUrls;

        @JsonProperty("accredited_it_employer")
        private Boolean accreditedItEmployer;

        private Boolean trusted;
    }
}