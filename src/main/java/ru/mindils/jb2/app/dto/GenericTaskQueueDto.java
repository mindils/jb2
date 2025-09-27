package ru.mindils.jb2.app.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Простой DTO для передачи данных задачи между Temporal Workflow и Activities
 * Без Jmix аннотаций для избежания проблем с сериализацией
 */
@Getter
@Setter
public class GenericTaskQueueDto {

  private Long id;
  private String entityName;
  private String entityId;
  private String taskType;
  private String status;
  private String errorMessage;
  private Integer priority;
  private OffsetDateTime createdDate;
  private OffsetDateTime lastModifiedDate;

  // Конструкторы
  public GenericTaskQueueDto() {
  }

  public GenericTaskQueueDto(Long id, String entityName, String entityId, String taskType, String status) {
    this.id = id;
    this.entityName = entityName;
    this.entityId = entityId;
    this.taskType = taskType;
    this.status = status;
  }

  @Override
  public String toString() {
    return String.format("GenericTaskQueueDto{id=%d, entityId='%s', taskType='%s', status='%s'}",
        id, entityId, taskType, status);
  }
}