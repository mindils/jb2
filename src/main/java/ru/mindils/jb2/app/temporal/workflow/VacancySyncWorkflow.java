package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;
import java.util.Map;

@WorkflowInterface
public interface VacancySyncWorkflow {
  @WorkflowMethod
  void run(List<Map<String, String>> requestParams);
}