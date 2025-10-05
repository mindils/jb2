package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;

@WorkflowInterface
public interface VacancyQueueProcessorWorkflow {

  @WorkflowMethod
  void processQueue(GenericTaskQueueType queueType);

  @SignalMethod
  void stop();
}