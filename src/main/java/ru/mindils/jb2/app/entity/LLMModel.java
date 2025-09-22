package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "JB2_LLM_MODEL", indexes = {
    @Index(name = "IDX_JB2_LLM_MODEL_PRIORITY", columnList = "priority_order"),
    @Index(name = "IDX_JB2_LLM_MODEL_ENABLED", columnList = "enabled"),
    @Index(name = "IDX_JB2_LLM_MODEL_IS_DEFAULT", columnList = "is_default")
})
@Entity(name = "jb2_LLMModel")
@Getter
@Setter
public class LLMModel {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "model_name", nullable = false)
  private String modelName;

  @NotNull
  @Column(name = "base_url", nullable = false)
  private String baseUrl;

  @Column(name = "api_key")
  private String apiKey;

  @Column(name = "priority_order", nullable = false)
  private Integer priorityOrder = 0;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @Column(name = "temperature")
  private Double temperature = 0.1;

  @Column(name = "max_tokens")
  private Integer maxTokens = 1000;

  @Column(name = "timeout_seconds")
  private Integer timeoutSeconds = 30;

  @Column(name = "failure_count")
  private Integer failureCount = 0;

  @Column(name = "last_failure")
  private OffsetDateTime lastFailure;

  @Column(name = "last_success")
  private OffsetDateTime lastSuccess;

  @Column(name = "available_after")
  private OffsetDateTime availableAfter;

  @Column(name = "description")
  private String description;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  public boolean isAvailable() {
    if (!enabled) return false;

    // Если установлено время до которого модель недоступна
    if (availableAfter != null && OffsetDateTime.now().isBefore(availableAfter)) {
      return false;
    }

    return true;
  }
}