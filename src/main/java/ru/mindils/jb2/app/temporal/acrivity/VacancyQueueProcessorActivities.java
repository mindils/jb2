package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import ru.mindils.jb2.app.dto.GenericTaskQueueDto;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;

import java.util.Optional;

@ActivityInterface
public interface VacancyQueueProcessorActivities {

  @ActivityMethod
  Optional<GenericTaskQueueDto> getNextTask(GenericTaskQueueType queueType);

  @ActivityMethod
  void updateTaskStatus(Long taskId, GenericTaskQueueStatus status, String errorMessage);

  @ActivityMethod
  void executeVacancyFirstAnalysisWorkflow(String vacancyId);

  @ActivityMethod
  void executeVacancyFullAnalysisWorkflow(String vacancyId);

  void executeVacancyUpdateWorkflow(String vacancyId);

}