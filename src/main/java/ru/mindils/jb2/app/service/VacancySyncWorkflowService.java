package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancySyncWorkflow;

import java.util.UUID;

@Service
public class VacancySyncWorkflowService {

  private final WorkflowClient workflowClient;

  public VacancySyncWorkflowService(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  public void sync() {
    VacancySyncWorkflow workflow = workflowClient.newWorkflowStub(
        VacancySyncWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancySyncConstants.VACANCY_QUEUE)
            .setWorkflowId(VacancySyncConstants.WORKFLOW_ID + "_" + UUID.randomUUID())
            .build()
    );

    WorkflowClient.start(workflow::sync);
  }

}
