package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VacancyUpdateWorkflow {

  @WorkflowMethod
  void updateVacancy(String vacancyId);
}