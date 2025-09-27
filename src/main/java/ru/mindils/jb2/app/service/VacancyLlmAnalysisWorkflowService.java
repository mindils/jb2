package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancyLlmAnalysisConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyLlmFirstAnalysisWorkflow;

@Service
public class VacancyLlmAnalysisWorkflowService {

  private final WorkflowClient workflowClient;
  private final TemporalStatusService temporalStatusService;

  public VacancyLlmAnalysisWorkflowService(WorkflowClient workflowClient, TemporalStatusService temporalStatusService) {
    this.workflowClient = workflowClient;
    this.temporalStatusService = temporalStatusService;
  }

  public void startFirstAnalysisBy(String vacancyId) {
    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_" + vacancyId;

    // Проверяем, не запущен ли уже такой workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("Workflow для цепочки " + vacancyId + " уже запущен");
    }

    VacancyLlmFirstAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyLlmFirstAnalysisWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyLlmAnalysisConstants.QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    WorkflowClient.start(workflow::run, vacancyId, false);
  }
}