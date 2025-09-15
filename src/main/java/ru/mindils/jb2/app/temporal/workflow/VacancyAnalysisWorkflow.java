package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import ru.mindils.jb2.app.entity.AnalysisType;

@WorkflowInterface
public interface VacancyAnalysisWorkflow {

  @WorkflowMethod
  void run(AnalysisType type);
}