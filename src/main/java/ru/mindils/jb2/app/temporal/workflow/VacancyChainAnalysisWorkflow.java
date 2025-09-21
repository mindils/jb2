package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import ru.mindils.jb2.app.service.analysis.chain.AnalysisChainConfig;

@WorkflowInterface
public interface VacancyChainAnalysisWorkflow {

  @WorkflowMethod
  void runChainAnalysis(AnalysisChainConfig config);
}