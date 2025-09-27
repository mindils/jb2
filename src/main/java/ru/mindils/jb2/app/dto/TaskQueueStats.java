package ru.mindils.jb2.app.dto;

import io.jmix.core.metamodel.annotation.JmixEntity;
import lombok.Getter;
import lombok.Setter;

@JmixEntity(name = "jb2_TaskQueueStats")
@Getter
@Setter
public class TaskQueueStats {

  private Integer newTasks = 0;
  private Integer processingTasks = 0;
  private Integer completedTasks = 0;
  private Integer failedTasks = 0;

  public int getTotalTasks() {
    return newTasks + processingTasks + completedTasks + failedTasks;
  }

  @Override
  public String toString() {
    return String.format("TaskQueueStats{total=%d, new=%d, processing=%d, completed=%d, failed=%d}",
        getTotalTasks(), newTasks, processingTasks, completedTasks, failedTasks);
  }
}