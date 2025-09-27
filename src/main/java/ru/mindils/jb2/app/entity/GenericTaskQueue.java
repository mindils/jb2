package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "JB2_GENERIC_TASK_QUEUE")
@Entity(name = "jb2_GenericTaskQueue")
@Getter
@Setter
public class GenericTaskQueue {
  @Column(name = "ID", nullable = false)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "entity_name")
  private String entityName;

  @Column(name = "ENTITY_ID")
  private String entityId;

  @Column(name = "TASK_TYPE")
  private String taskType;

  @Column(name = "STATUS")
  private String status;

  @Column(name = "error_message")
  @Lob
  private String errorMessage;

  @Column(name = "PRIORITY")
  private Integer priority;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  public GenericTaskQueueStatus getStatus() {
    return status == null ? null : GenericTaskQueueStatus.fromId(status);
  }

  public void setStatus(GenericTaskQueueStatus status) {
    this.status = status == null ? null : status.getId();
  }

  public boolean isNew() {
    return GenericTaskQueueStatus.NEW.getId().equals(status);
  }

  public boolean isProcessing() {
    return GenericTaskQueueStatus.PROCESSING.getId().equals(status);
  }

  public boolean isCompleted() {
    return GenericTaskQueueStatus.COMPLETED.getId().equals(status);
  }

  public boolean isFailed() {
    return GenericTaskQueueStatus.FAILED.getId().equals(status);
  }
}