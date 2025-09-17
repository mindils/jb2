package ru.mindils.jb2.app.dto;

import io.jmix.core.metamodel.annotation.JmixEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@JmixEntity(name = "jb2_WorkflowInfo")
@Getter
@Setter
public class WorkflowInfo {
  private String workflowId;
  private String workflowType;
  private String status;
  private LocalDateTime startTime;
  private String description;
  private String runId;
}