package ru.mindils.jb2.app.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import ru.mindils.jb2.app.util.converter.JsonNodeConverter;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "JB2_VACANCY_ANALYSIS")
@Entity(name = "jb2_VacancyAnalysis")
@DdlGeneration(unmappedConstraints = {"FK_JB2_VACANCY_ANALYSIS_ON_ID"})
@Getter
@Setter
public class VacancyAnalysis {
  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
  @PrimaryKeyJoinColumn
  private Vacancy vacancy;

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "step_results", columnDefinition = "jsonb")
  private JsonNode stepResults;

  // Итоговая оценка
  @Column(name = "final_score")
  private Integer finalScore;

  // Рейтинг (EXCELLENT, GOOD, etc.)
  @Column(name = "rating")
  private String rating;

  // Метаданные последнего анализа
  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "analysis_metadata", columnDefinition = "jsonb")
  private JsonNode analysisMetadata;

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private OffsetDateTime lastModifiedDate;

  /**
   * Получить результат конкретного шага
   */
  public JsonNode getStepResult(String stepId) {
    if (stepResults == null || !stepResults.isObject()) {
      return null;
    }
    return stepResults.get(stepId);
  }

  /**
   * Проверить, выполнялся ли шаг
   */
  public boolean hasStepResult(String stepId) {
    return getStepResult(stepId) != null;
  }


  /**
   * Получить значение Java из новой структуры или старого поля
   */
  @JmixProperty
  public Boolean getJavaValue() {
    JsonNode primaryResult = getStepResult("primary");
    if (primaryResult != null && primaryResult.has("java")) {
      return primaryResult.get("java").asBoolean();
    }
    return null;
  }

  /**
   * Получить значение Jmix из новой структуры или старого поля
   */
  @JmixProperty
  public Boolean getJmixValue() {
    JsonNode primaryResult = getStepResult("primary");
    if (primaryResult != null && primaryResult.has("jmix")) {
      return primaryResult.get("jmix").asBoolean();
    }
    return null;
  }

  /**
   * Получить значение AI из новой структуры или старого поля
   */
  @JmixProperty
  public Boolean getAiValue() {
    JsonNode primaryResult = getStepResult("primary");
    if (primaryResult != null && primaryResult.has("ai")) {
      return primaryResult.get("ai").asBoolean();
    }
    return null;
  }

  /**
   * Получить формат работы
   */
  @JmixProperty
  public String getWorkModeValue() {
    JsonNode socialResult = getStepResult("social");
    if (socialResult != null && socialResult.has("work_mode")) {
      return socialResult.get("work_mode").asText();
    }
    return null;
  }

  /**
   * Получить социальную значимость
   */
  @JmixProperty
  public Boolean getSociallySignificantValue() {
    JsonNode socialResult = getStepResult("social");
    if (socialResult != null && socialResult.has("socially_significant")) {
      return socialResult.get("socially_significant").asBoolean();
    }
    return null;
  }

  /**
   * Получить рейтинг как enum
   */
  @JmixProperty
  public VacancyRating getRatingEnum() {
    if (rating == null) return null;
    try {
      return VacancyRating.valueOf(rating);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public void setRatingEnum(VacancyRating ratingEnum) {
    this.rating = ratingEnum != null ? ratingEnum.name() : null;
  }
}