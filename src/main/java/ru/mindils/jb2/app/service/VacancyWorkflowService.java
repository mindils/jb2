package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.temporal.VacancyAnalysisConstants;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.VacancyUpdateConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyAnalysisWorkflow;
import ru.mindils.jb2.app.temporal.workflow.VacancySyncWorkflow;
import ru.mindils.jb2.app.temporal.workflow.VacancyUpdateWorkflow;

import java.util.List;
import java.util.Map;

@Service
public class VacancyWorkflowService {
  private final WorkflowClient workflowClient;

  public VacancyWorkflowService(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  // Оригинальный метод без параметров (для обратной совместимости)
  public void sync() {
    sync(null);
  }

  // Новый метод с параметрами
  public void sync(List<Map<String, String>> requestParams) {
    VacancySyncWorkflow workflow = workflowClient.newWorkflowStub(
        VacancySyncWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancySyncConstants.VACANCY_QUEUE)
            .setWorkflowId(VacancySyncConstants.WORKFLOW_ID)
            .build()
    );

    WorkflowClient.start(() -> workflow.run(requestParams));
  }

  public void updateFromQueue() {
    VacancyUpdateWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyUpdateWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyUpdateConstants.QUEUE)
            .setWorkflowId(VacancyUpdateConstants.WORKFLOW_ID)
            .build()
    );

    WorkflowClient.start(workflow::run);
  }

  public void analyze(AnalysisType type) {
    VacancyAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyAnalysisWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyAnalysisConstants.QUEUE)
            .setWorkflowId(VacancyAnalysisConstants.WORKFLOW_ID + "_" + type.getId())
            .build()
    );

    WorkflowClient.start(() -> workflow.run(type));
  }
}