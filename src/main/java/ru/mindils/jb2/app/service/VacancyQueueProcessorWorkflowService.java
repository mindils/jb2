package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancyQueueProcessorConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyQueueProcessorWorkflow;

@Service
public class VacancyQueueProcessorWorkflowService {

  private final WorkflowClient workflowClient;
  private final TemporalStatusService temporalStatusService;

  public VacancyQueueProcessorWorkflowService(WorkflowClient workflowClient,
                                              TemporalStatusService temporalStatusService) {
    this.workflowClient = workflowClient;
    this.temporalStatusService = temporalStatusService;
  }

  /**
   * Запускает обработку очереди вакансий для первичного анализа
   */
  public void startQueueProcessing() {
    String workflowId = VacancyQueueProcessorConstants.WORKFLOW_ID;

    // Проверяем, не запущен ли уже такой workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("Queue processor workflow уже запущен");
    }

    VacancyQueueProcessorWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyQueueProcessorWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyQueueProcessorConstants.QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    WorkflowClient.start(workflow::processQueue);
  }

  /**
   * Проверяет, запущен ли workflow обработки очереди
   */
  public boolean isQueueProcessorRunning() {
    return temporalStatusService.isWorkflowRunning(VacancyQueueProcessorConstants.WORKFLOW_ID);
  }
}