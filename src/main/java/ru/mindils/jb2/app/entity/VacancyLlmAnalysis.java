package ru.mindils.jb2.app.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import ru.mindils.jb2.app.util.converter.JsonNodeConverter;

import java.time.OffsetDateTime;
import java.util.UUID;

@DdlGeneration(unmappedConstraints = {"fk_jb2_vacancy_llm_analysis_on_vacancy"})
@JmixEntity
@Table(name = "jb2_vacancy_llm_analysis", indexes = {
    @Index(name = "idx_jb2_vacancy_llm_analysis_vacancy", columnList = "vacancy_id")
})
@Entity(name = "jb2_VacancyLlmAnalysis")
@Getter
@Setter
public class VacancyLlmAnalysis {
  @Id
  @Column(name = "uid", nullable = false)
  private UUID id;

  @Column(name = "LLM_MODEL")
  private String llmModel;

  @Column(name = "LLM_CALL_LOG_ID")
  private Long llmCallLogId;

  @JoinColumn(name = "vacancy_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Vacancy vacancy;

  @Column(name = "analyze_type")
  private String analyzeType;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "analyze_data", columnDefinition = "jsonb")
  private JsonNode analyzeData;

  @Column(name = "analyze_data_string")
  @Lob
  private String analyzeDataString;

  @Column(name = "status")
  private String status;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  public VacancyLlmAnalysisStatus getStatus() {
    return status == null ? null : VacancyLlmAnalysisStatus.fromId(status);
  }

  public void setStatus(VacancyLlmAnalysisStatus status) {
    this.status = status == null ? null : status.getId();
  }


}