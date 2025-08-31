package ru.mindils.jb2.app.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import ru.mindils.jb2.app.util.converter.JsonNodeConverter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@JmixEntity
@Table(name = "JB2_VACANCY")
@Entity(name = "jb2_Vacancy")
@Getter
@Setter
public class Vacancy {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @InstanceName
  @Column(name = "name")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  @Lob
  private String description;

  @Column(name = "branded_description", columnDefinition = "TEXT")
  @Lob
  private String brandedDescription;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id")
  private Employer employer;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "billing_type", columnDefinition = "jsonb")
  private JsonNode billingType;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "area", columnDefinition = "jsonb")
  private JsonNode area;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "salary", columnDefinition = "jsonb")
  private JsonNode salary;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "address", columnDefinition = "jsonb")
  private JsonNode address;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "experience", columnDefinition = "jsonb")
  private JsonNode experience;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "schedule", columnDefinition = "jsonb")
  private JsonNode schedule;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "employment", columnDefinition = "jsonb")
  private JsonNode employment;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "key_skills", columnDefinition = "jsonb")
  private JsonNode keySkills;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "professional_roles", columnDefinition = "jsonb")
  private JsonNode professionalRoles;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "working_time_modes", columnDefinition = "jsonb")
  private JsonNode workingTimeModes;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "work_format", columnDefinition = "jsonb")
  private JsonNode workFormat;

  @Column(name = "premium")
  private Boolean premium;

  @Column(name = "response_letter_required")
  private Boolean responseLetterRequired;

  @Column(name = "allow_messages")
  private Boolean allowMessages;

  @Column(name = "has_test")
  private Boolean hasTest;

  @Column(name = "archived")
  private Boolean archived;

  @Column(name = "hidden")
  private Boolean hidden;

  @Column(name = "accept_handicapped")
  private Boolean acceptHandicapped;

  @Column(name = "accept_kids")
  private Boolean acceptKids;

  @Column(name = "accept_incomplete_resumes")
  private Boolean acceptIncompleteResumes;

  @Column(name = "quick_responses_allowed")
  private Boolean quickResponsesAllowed;

  @Column(name = "approved")
  private Boolean approved;

  @Column(name = "internship")
  private Boolean internship;

  @Column(name = "night_shifts")
  private Boolean nightShifts;

  @Column(name = "accept_temporary")
  private Boolean acceptTemporary;

  @Column(name = "show_logo_in_search")
  private Boolean showLogoInSearch;

  @Column(name = "closed_for_applicants")
  private Boolean closedForApplicants;

  @Column(name = "response_url")
  private String responseUrl;

  @Column(name = "apply_alternate_url")
  private String applyAlternateUrl;

  @Column(name = "alternate_url")
  private String alternateUrl;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "initial_created_at")
  private LocalDateTime initialCreatedAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @CreatedBy
  @Column(name = "CREATED_BY")
  private String createdBy;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedBy
  @Column(name = "LAST_MODIFIED_BY")
  private String lastModifiedBy;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  @OneToOne(mappedBy = "vacancy", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private VacancyInfo vacancyInfo;

  @JmixProperty
  public String getKeySkillsStr() {
    if (keySkills == null || !keySkills.isArray()) {
      return "";
    }

    return StreamSupport.stream(keySkills.spliterator(), false)
        .map(node -> node.path("name").asText())
        .filter(name -> !name.isEmpty())
        .collect(Collectors.joining(", "));
  }

  @JmixProperty
  public String getCity() {
    return area.path("name").asText();
  }

  @JmixProperty
  public String getMetro() {
    return address.path("metro").path("station_name").asText();
  }

  @JmixProperty
  public String getSalaryStr() {
    if (salary == null) {
      return "";
    }

    return salary.asText();
  }

  @JmixProperty
  public String getProfessionalRolesStr() {
    if (professionalRoles == null || !professionalRoles.isArray()) {
      return "";
    }

    return StreamSupport.stream(professionalRoles.spliterator(), false)
        .map(node -> node.path("name").asText())
        .filter(name -> !name.isEmpty())
        .collect(Collectors.joining(", "));
  }

  @JmixProperty
  public String getWorkFormatStr() {
    if (workFormat == null || !workFormat.isArray()) {
      return "";
    }

    return StreamSupport.stream(workFormat.spliterator(), false)
        .map(node -> node.path("name").asText())
        .filter(name -> !name.isEmpty())
        .collect(Collectors.joining(", "));
  }
}