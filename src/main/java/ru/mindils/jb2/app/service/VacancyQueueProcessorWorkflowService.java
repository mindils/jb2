package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
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
   * Запускает обработку очереди для указанного типа задач
   */
  public void startQueueProcessing(GenericTaskQueueType queueType) {
    if (queueType == null) {
      throw new IllegalArgumentException("queueType cannot be null");
    }

    String workflowId = VacancyQueueProcessorConstants.WORKFLOW_ID + "_" + queueType.getId();

    // Проверяем, не запущен ли уже такой workflow
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      throw new IllegalStateException("Queue processor workflow для типа " + queueType + " уже запущен");
    }

    VacancyQueueProcessorWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyQueueProcessorWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancyQueueProcessorConstants.QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    WorkflowClient.start(workflow::processQueue, queueType);
  }

  /**
   * Останавливает обработку очереди для указанного типа через сигнал
   * @return true если сигнал был отправлен, false если workflow не запущен
   */
  public boolean stopQueueProcessing(GenericTaskQueueType queueType) {
    try {
      String workflowId = VacancyQueueProcessorConstants.WORKFLOW_ID + "_" + queueType.getId();

      // Проверяем, запущен ли workflow
      if (!temporalStatusService.isWorkflowRunning(workflowId)) {
        return false;
      }

      // Получаем существующий workflow stub
      VacancyQueueProcessorWorkflow workflow = workflowClient.newWorkflowStub(
          VacancyQueueProcessorWorkflow.class,
          workflowId
      );

      // Отправляем сигнал остановки
      workflow.stop();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Запускает обработку очереди для первичного анализа
   */
  public void startFirstAnalysisQueueProcessing() {
    startQueueProcessing(GenericTaskQueueType.LLM_FIRST);
  }

  /**
   * Запускает обработку очереди для полного анализа
   */
  public void startFullAnalysisQueueProcessing() {
    startQueueProcessing(GenericTaskQueueType.LLM_FULL);
  }

  /**
   * Запускает обработку очереди для обновления вакансий
   */
  public void startVacancyUpdateQueueProcessing() {
    startQueueProcessing(GenericTaskQueueType.VACANCY_UPDATE);
  }

  /**
   * Останавливает обработку очереди для первичного анализа
   */
  public boolean stopFirstAnalysisQueueProcessing() {
    return stopQueueProcessing(GenericTaskQueueType.LLM_FIRST);
  }

  /**
   * Останавливает обработку очереди для полного анализа
   */
  public boolean stopFullAnalysisQueueProcessing() {
    return stopQueueProcessing(GenericTaskQueueType.LLM_FULL);
  }

  /**
   * Останавливает обработку очереди для обновления вакансий
   */
  public boolean stopVacancyUpdateQueueProcessing() {
    return stopQueueProcessing(GenericTaskQueueType.VACANCY_UPDATE);
  }

  /**
   * Проверяет, запущен ли workflow обработки очереди для указанного типа
   */
  public boolean isQueueProcessorRunning(GenericTaskQueueType queueType) {
    String workflowId = VacancyQueueProcessorConstants.WORKFLOW_ID + "_" + queueType.getId();
    return temporalStatusService.isWorkflowRunning(workflowId);
  }

  /**
   * Проверяет, запущен ли workflow обработки очереди первичного анализа
   */
  public boolean isFirstAnalysisQueueProcessorRunning() {
    return isQueueProcessorRunning(GenericTaskQueueType.LLM_FIRST);
  }

  /**
   * Проверяет, запущен ли workflow обработки очереди полного анализа
   */
  public boolean isFullAnalysisQueueProcessorRunning() {
    return isQueueProcessorRunning(GenericTaskQueueType.LLM_FULL);
  }

  /**
   * Проверяет, запущен ли workflow обработки очереди обновления вакансий
   */
  public boolean isVacancyUpdateQueueProcessorRunning() {
    return isQueueProcessorRunning(GenericTaskQueueType.VACANCY_UPDATE);
  }

  @Deprecated
  public boolean isQueueProcessorRunning() {
    return isFirstAnalysisQueueProcessorRunning();
  }
}