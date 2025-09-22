package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@JmixEntity(name = "jb2_LLMModelStats")
@Getter
@Setter
public class LLMModelStats {

  private Long id;
  private String name;
  private String modelName;
  private Boolean enabled;
  private Boolean isDefault;
  private Integer priority;
  private Integer failureCount;
  private OffsetDateTime lastFailure;
  private OffsetDateTime lastSuccess;
  private OffsetDateTime availableAfter;
  private Boolean available;
  private String description;

  public LLMModelStats() {}

  public LLMModelStats(LLMModel model) {
    this.id = model.getId();
    this.name = model.getName();
    this.modelName = model.getModelName();
    this.enabled = model.getEnabled();
    this.isDefault = model.getIsDefault();
    this.priority = model.getPriorityOrder();
    this.failureCount = model.getFailureCount() != null ? model.getFailureCount() : 0;
    this.lastFailure = model.getLastFailure();
    this.lastSuccess = model.getLastSuccess();
    this.availableAfter = model.getAvailableAfter();
    this.available = model.isAvailable();
    this.description = model.getDescription();
  }
}