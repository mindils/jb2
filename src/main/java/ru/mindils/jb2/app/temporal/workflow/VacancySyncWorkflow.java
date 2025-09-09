package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VacancySyncWorkflow {

  @WorkflowMethod
  void sync();
}
