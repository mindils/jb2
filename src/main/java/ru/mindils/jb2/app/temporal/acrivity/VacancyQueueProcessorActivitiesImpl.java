package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.GenericTaskQueueDto;
import ru.mindils.jb2.app.entity.GenericTaskQueue;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.mapper.GenericTaskQueueMapper;
import ru.mindils.jb2.app.service.TemporalStatusService;
import ru.mindils.jb2.app.temporal.VacancyLlmAnalysisConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyLlmFirstAnalysisWorkflow;
import ru.mindils.jb2.app.temporal.workflow.VacancyLlmFullAnalysisWorkflow;

import java.util.Optional;

@Component
@ActivityImpl(taskQueues = "vacancy-queue-processor")
public class VacancyQueueProcessorActivitiesImpl implements VacancyQueueProcessorActivities {

  private static final Logger log = LoggerFactory.getLogger(VacancyQueueProcessorActivitiesImpl.class);

  private final DataManager dataManager;
  private final WorkflowClient workflowClient;
  private final TemporalStatusService temporalStatusService;
  private final SystemAuthenticator authenticator;
  private final GenericTaskQueueMapper taskQueueMapper;

  public VacancyQueueProcessorActivitiesImpl(DataManager dataManager,
                                             WorkflowClient workflowClient,
                                             SystemAuthenticator authenticator,
                                             GenericTaskQueueMapper taskQueueMapper,
                                             TemporalStatusService temporalStatusService) {
    this.dataManager = dataManager;
    this.workflowClient = workflowClient;
    this.temporalStatusService = temporalStatusService;
    this.authenticator = authenticator;
    this.taskQueueMapper = taskQueueMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<GenericTaskQueueDto> getNextTask(GenericTaskQueueType queueType) {
    log.debug("Looking for next {} task in queue", queueType);

    return authenticator.withSystem(() -> {
      Optional<GenericTaskQueue> optional = dataManager.load(GenericTaskQueue.class)
          .query("select t from jb2_GenericTaskQueue t " +
              "where t.taskType = :taskType " +
              "and t.status = :status " +
              "order by t.priority asc, t.createdDate asc")
          .parameter("taskType", queueType.getId())
          .parameter("status", GenericTaskQueueStatus.NEW.getId())
          .maxResults(1)
          .optional();
      return optional.map(taskQueueMapper::toDto);
    });
  }

  @Override
  @Transactional
  public void updateTaskStatus(Long taskId, GenericTaskQueueStatus status, String errorMessage) {
    log.debug("Updating task {} status to: {}", taskId, status.getId());

    authenticator.runWithSystem(() -> {
      GenericTaskQueue task = dataManager.load(GenericTaskQueue.class).id(taskId).one();
      task.setStatus(status);
      task.setErrorMessage(errorMessage);
      dataManager.save(task);
    });
  }

  @Override
  public void executeVacancyFirstAnalysisWorkflow(String vacancyId) {
    log.info("Starting VacancyLlmFirstAnalysisWorkflow for vacancy: {}", vacancyId);

    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_first_" + vacancyId;

    // Проверяем, не запущен ли уже workflow для этой вакансии
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      log.warn("First analysis workflow for vacancy {} is already running, skipping", vacancyId);
      throw new IllegalStateException("First analysis workflow для вакансии " + vacancyId + " уже запущен");
    }

    try {
      VacancyLlmFirstAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
          VacancyLlmFirstAnalysisWorkflow.class,
          WorkflowOptions.newBuilder()
              .setTaskQueue(VacancyLlmAnalysisConstants.QUEUE)
              .setWorkflowId(workflowId)
              .build()
      );

      // Запускаем workflow синхронно
      workflow.run(vacancyId, false);

      log.info("VacancyLlmFirstAnalysisWorkflow completed successfully for vacancy: {}", vacancyId);

    } catch (Exception e) {
      log.error("Error during VacancyLlmFirstAnalysisWorkflow execution for vacancy {}: {}",
          vacancyId, e.getMessage(), e);
      throw new RuntimeException("First analysis workflow failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void executeVacancyFullAnalysisWorkflow(String vacancyId) {
    log.info("Starting VacancyLlmFullAnalysisWorkflow for vacancy: {}", vacancyId);

    String workflowId = VacancyLlmAnalysisConstants.WORKFLOW_ID + "_full_" + vacancyId;

    // Проверяем, не запущен ли уже workflow для этой вакансии
    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      log.warn("Full analysis workflow for vacancy {} is already running, skipping", vacancyId);
      throw new IllegalStateException("Full analysis workflow для вакансии " + vacancyId + " уже запущен");
    }

    try {
      VacancyLlmFullAnalysisWorkflow workflow = workflowClient.newWorkflowStub(
          VacancyLlmFullAnalysisWorkflow.class,
          WorkflowOptions.newBuilder()
              .setTaskQueue(VacancyLlmAnalysisConstants.QUEUE)
              .setWorkflowId(workflowId)
              .build()
      );

      // Запускаем workflow синхронно (refresh=false, используем существующие данные)
      workflow.run(vacancyId, false);

      log.info("VacancyLlmFullAnalysisWorkflow completed successfully for vacancy: {}", vacancyId);

    } catch (Exception e) {
      log.error("Error during VacancyLlmFullAnalysisWorkflow execution for vacancy {}: {}",
          vacancyId, e.getMessage(), e);
      throw new RuntimeException("Full analysis workflow failed: " + e.getMessage(), e);
    }
  }
}