package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancyLlmAnalysisConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyLlmFirstAnalysisWorkflow;
import ru.mindils.jb2.app.temporal.workflow.VacancyLlmFullAnalysisWorkflow;

@Service
public class VacancyLlmAnalysisWorkflowService {

  private final WorkflowClient workflowClient;
  private final TemporalStatusService temporalStatusService;

  public VacancyLlmAnalysisWorkflowService(WorkflowClient workflowClient, TemporalStatusService temporalStatusService) {
    this.workflowClient = workflowClient;
    this.temporalStatusService = temporalStatusService;
  }

  /**
   * Запускает первичный анализ (только JAVA_PRIMARY)
   */
  public void startFirstAnalysisBy(String vacancyId) {
    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_first_" + vacancyId;

    // Проверяем, не запущен ли уже такой workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("First analysis workflow для вакансии " + vacancyId + " уже запущен");
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

  /**
   * Запускает полный анализ (все типы анализа)
   */
  public void startFullAnalysisBy(String vacancyId) {
    startFullAnalysisBy(vacancyId, false);
  }

  /**
   * Запускает полный анализ с возможностью принудительного обновления
   *
   * @param vacancyId ID вакансии для анализа
   * @param refresh если true, то перезапускает анализ даже если есть существующие результаты
   */
  public void startFullAnalysisBy(String vacancyId, Boolean refresh) {
    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_full_" + vacancyId;

    // Проверяем, не запущен ли уже такой workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("Full analysis workflow для вакансии " + vacancyId + " уже запущен");
    }

    VacancyLlmFullAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyLlmFullAnalysisWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyLlmAnalysisConstants.QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    WorkflowClient.start(workflow::run, vacancyId, refresh);
  }

  /**
   * Проверяет, запущен ли первичный анализ для данной вакансии
   */
  public boolean isFirstAnalysisRunning(String vacancyId) {
    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_first_" + vacancyId;
    return temporalStatusService.isWorkflowRunning(workflowId);
  }

  /**
   * Проверяет, запущен ли полный анализ для данной вакансии
   */
  public boolean isFullAnalysisRunning(String vacancyId) {
    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_full_" + vacancyId;
    return temporalStatusService.isWorkflowRunning(workflowId);
  }

  /**
   * Проверяет, запущен ли любой анализ для данной вакансии
   */
  public boolean isAnyAnalysisRunning(String vacancyId) {
    return isFirstAnalysisRunning(vacancyId) || isFullAnalysisRunning(vacancyId);
  }
}