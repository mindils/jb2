package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancyAnalysisConstants;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyAnalysisWorkflow;
import ru.mindils.jb2.app.temporal.workflow.VacancySyncWorkflow;

@Service
public class VacancyWorkflowService {

  private final WorkflowClient workflowClient;

  public VacancyWorkflowService(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  public void sync() {
    VacancySyncWorkflow workflow = workflowClient.newWorkflowStub(
        VacancySyncWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancySyncConstants.VACANCY_QUEUE)
            // делаем одинаковый workflow чтобы можно было запустить только один раз.
            .setWorkflowId(VacancySyncConstants.WORKFLOW_ID)
            .build()
    );

    WorkflowClient.start(workflow::run);
  }

  public void analyze() {
    VacancyAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyAnalysisWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyAnalysisConstants.QUEUE)
            // делаем одинаковый workflow чтобы можно было запустить только один раз.
            .setWorkflowId(VacancyAnalysisConstants.WORKFLOW_ID)
            .build()
    );

    WorkflowClient.start(workflow::run);
  }

}
