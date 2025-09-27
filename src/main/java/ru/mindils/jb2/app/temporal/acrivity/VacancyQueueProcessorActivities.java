package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import ru.mindils.jb2.app.dto.GenericTaskQueueDto;
import ru.mindils.jb2.app.entity.GenericTaskQueue;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;

import java.util.Optional;

@ActivityInterface
public interface VacancyQueueProcessorActivities {

  @ActivityMethod
  Optional<GenericTaskQueueDto> getNextLlmFirstTask();

  @ActivityMethod
  void updateTaskStatus(Long taskId, GenericTaskQueueStatus status, String errorMessage);

  @ActivityMethod
  void executeVacancyAnalysisWorkflow(String vacancyId);
}