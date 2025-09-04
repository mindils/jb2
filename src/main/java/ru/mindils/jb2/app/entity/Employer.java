package ru.mindils.jb2.app.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import ru.mindils.jb2.app.util.converter.JsonNodeConverter;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@JmixEntity
@Table(name = "JB2_EMPLOYER")
@Entity(name = "jb2_Employer")
@Getter
@Setter
public class Employer {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @InstanceName
  @Column(name = "name")
  private String name;

  @Column(name = "trusted")
  private Boolean trusted;

  @Column(name = "accredited_it_employer")
  private Boolean accreditedItEmployer;

  @Column(name = "has_divisions")
  private Boolean hasDivisions;

  @Column(name = "type")
  private String type;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "site_url")
  private String siteUrl;

  @Column(name = "alternate_url")
  private String alternateUrl;

  @Column(name = "vacancies_url")
  private String vacanciesUrl;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "logo_urls", columnDefinition = "jsonb")
  private JsonNode logoUrls;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "relations", columnDefinition = "jsonb")
  private JsonNode relations;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "area", columnDefinition = "jsonb")
  private JsonNode area;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "industries", columnDefinition = "jsonb")
  private JsonNode industries;

  @Column(name = "branded_description", columnDefinition = "TEXT")
  private String brandedDescription;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "branding", columnDefinition = "jsonb")
  private JsonNode branding;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "insider_interviews", columnDefinition = "jsonb")
  private JsonNode insiderInterviews;

  @Column(name = "open_vacancies")
  private Integer openVacancies;

  @OneToOne(mappedBy = "employer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private EmployerInfo employerInfo;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  @JmixProperty
  public String getLogoUrl240() {
    return logoUrls.path("240").asText();
  }

  @JmixProperty
  public String getCity() {
    return area.path("name").asText();
  }

  @JmixProperty
  public String getIndustriesStr() {
    if (industries== null || !industries.isArray()) {
      return "";
    }

    return StreamSupport.stream(industries.spliterator(), false)
        .map(node -> node.path("name").asText())
        .filter(name -> !name.isEmpty())
        .collect(Collectors.joining(", "));
  }


}