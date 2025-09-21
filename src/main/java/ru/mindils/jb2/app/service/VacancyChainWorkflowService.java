package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.service.analysis.chain.AnalysisChainConfig;
import ru.mindils.jb2.app.temporal.VacancyChainAnalysisConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyChainAnalysisWorkflow;

/**
 * Сервис для запуска workflow цепочки анализа
 */
@Service
public class VacancyChainWorkflowService {

  private final WorkflowClient workflowClient;
  private final TemporalStatusService temporalStatusService;

  public VacancyChainWorkflowService(WorkflowClient workflowClient, TemporalStatusService temporalStatusService) {
    this.workflowClient = workflowClient;
    this.temporalStatusService = temporalStatusService;
  }

  /**
   * Запустить полный анализ (все шаги)
   */
  public void startFullAnalysis() {
    startChainAnalysis(AnalysisChainConfig.FULL_ANALYSIS);
  }

  /**
   * Запустить только первичный анализ
   */
  public void startPrimaryAnalysis() {
    startChainAnalysis(AnalysisChainConfig.PRIMARY_ONLY);
  }

  /**
   * Запустить социальный + технический анализ
   */
  public void startSocialTechnicalAnalysis() {
    startChainAnalysis(AnalysisChainConfig.SOCIAL_TECHNICAL);
  }

  /**
   * Запустить анализ с кастомной конфигурацией
   */
  public void startChainAnalysis(AnalysisChainConfig config) {
    String workflowId = VacancyChainAnalysisConstants.WORKFLOW_ID_PREFIX + "_" + config.chainId();

    // Проверяем, не запущен ли уже такой workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("Workflow для цепочки " + config.chainId() + " уже запущен");
    }

    VacancyChainAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyChainAnalysisWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyChainAnalysisConstants.QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    WorkflowClient.start(() -> workflow.runChainAnalysis(config));
  }

  /**
   * Проверить, запущен ли workflow для определенной цепочки
   */
  public boolean isChainAnalysisRunning(String chainId) {
    String workflowId = VacancyChainAnalysisConstants.WORKFLOW_ID_PREFIX + "_" + chainId;
    return temporalStatusService.isWorkflowRunning(workflowId);
  }
}