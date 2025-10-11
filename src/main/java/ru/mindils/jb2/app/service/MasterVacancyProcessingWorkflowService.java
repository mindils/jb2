package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.MasterVacancyProcessingConstants;
import ru.mindils.jb2.app.temporal.workflow.MasterVacancyProcessingWorkflow;

@Service
public class MasterVacancyProcessingWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(MasterVacancyProcessingWorkflowService.class);

  private final WorkflowClient workflowClient;
  private final TemporalStatusService temporalStatusService;

  public MasterVacancyProcessingWorkflowService(
      WorkflowClient workflowClient,
      TemporalStatusService temporalStatusService) {
    this.workflowClient = workflowClient;
    this.temporalStatusService = temporalStatusService;
  }

  /**
   * Запускает полный цикл обработки вакансий
   *
   * @param syncDays количество дней для синхронизации недавних вакансий (обычно 1)
   */
  public void startMasterProcessing(int syncDays) {
    String workflowId = MasterVacancyProcessingConstants.WORKFLOW_ID;

    // Проверяем, не запущен ли уже workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("Master vacancy processing workflow уже запущен");
    }

    log.info("Starting master vacancy processing workflow with syncDays={}", syncDays);

    MasterVacancyProcessingWorkflow workflow = workflowClient.newWorkflowStub(
        MasterVacancyProcessingWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(MasterVacancyProcessingConstants.QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    // Запускаем асинхронно
    WorkflowClient.start(workflow::processAllVacancies, syncDays);

    log.info("Master vacancy processing workflow started successfully");
  }

  /**
   * Запускает обработку с синхронизацией за последний день (по умолчанию)
   */
  public void startMasterProcessing() {
    startMasterProcessing(1);
  }

  /**
   * Проверяет, запущен ли master workflow
   */
  public boolean isProcessing() {
    String workflowId = MasterVacancyProcessingConstants.WORKFLOW_ID;
    return temporalStatusService.isWorkflowRunning(workflowId);
  }
}