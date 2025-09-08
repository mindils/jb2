package ru.mindils.jb2.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class VacancyShortDto {
  private String id;
  private Boolean premium;
  private String name;
  private String department;
  @JsonProperty("has_test")
  private Boolean hasTest;
  @JsonProperty("response_letter_required")
  private Boolean responseLetterRequired;
  private Map<String, Object> area;
  private Map<String, Object> salary;
  @JsonProperty("salary_range")
  private SalaryRangeDto salaryRange;
  private Map<String, Object> type;
  private Map<String, Object> address;
  @JsonProperty("response_url")
  private String responseUrl;
  @JsonProperty("sort_point_distance")
  private Double sortPointDistance;
  @JsonProperty("published_at")
  private OffsetDateTime publishedAt;
  @JsonProperty("created_at")
  private OffsetDateTime createdAt;
  private Boolean archived;
  @JsonProperty("apply_alternate_url")
  private String applyAlternateUrl;
  private Map<String, Object> branding;
  @JsonProperty("show_logo_in_search")
  private Boolean showLogoInSearch;
  @JsonProperty("show_contacts")
  private Boolean showContacts;
  @JsonProperty("insider_interview")
  private Object insiderInterview;
  private String url;
  @JsonProperty("alternate_url")
  private String alternateUrl;
  private List<Object> relations;
  private EmployerShortDto employer;
  private SnippetDto snippet;
  private Object contacts;
  private Map<String, Object> schedule;
  @JsonProperty("working_days")
  private List<Object> workingDays;
  @JsonProperty("working_time_intervals")
  private List<Object> workingTimeIntervals;
  @JsonProperty("working_time_modes")
  private List<Object> workingTimeModes;
  @JsonProperty("accept_temporary")
  private Boolean acceptTemporary;
  @JsonProperty("fly_in_fly_out_duration")
  private List<Object> flyInFlyOutDuration;
  @JsonProperty("work_format")
  private List<Map<String, String>> workFormat;
  @JsonProperty("working_hours")
  private List<Map<String, String>> workingHours;
  @JsonProperty("work_schedule_by_days")
  private List<Map<String, String>> workScheduleByDays;
  @JsonProperty("night_shifts")
  private Boolean nightShifts;
  @JsonProperty("professional_roles")
  private List<Map<String, String>> professionalRoles;
  @JsonProperty("accept_incomplete_resumes")
  private Boolean acceptIncompleteResumes;
  private Map<String, Object> experience;
  private Map<String, Object> employment;
  @JsonProperty("employment_form")
  private Map<String, Object> employmentForm;
  private Boolean internship;
  @JsonProperty("adv_response_url")
  private String advResponseUrl;
  @JsonProperty("is_adv_vacancy")
  private Boolean isAdvVacancy;
  @JsonProperty("adv_context")
  private Object advContext;

  @Data
  public static class EmployerShortDto {
    private String id;
    private String name;
    private String url;
    @JsonProperty("alternate_url")
    private String alternateUrl;
    @JsonProperty("logo_urls")
    private Map<String, String> logoUrls;
    @JsonProperty("vacancies_url")
    private String vacanciesUrl;
    @JsonProperty("accredited_it_employer")
    private Boolean accreditedItEmployer;
    private Boolean trusted;
  }

  @Data
  public static class SnippetDto {
    private String requirement;
    private String responsibility;
  }

  @Data
  public static class SalaryRangeDto {
    private Integer from;
    private Integer to;
    private String currency;
    private Boolean gross;
    private Map<String, Object> mode;
    private Map<String, Object> frequency;
  }
}