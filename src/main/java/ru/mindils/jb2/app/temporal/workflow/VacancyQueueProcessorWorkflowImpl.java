package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.dto.GenericTaskQueueDto;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.temporal.VacancyQueueProcessorConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyQueueProcessorActivities;

import java.time.Duration;
import java.util.Optional;

@WorkflowImpl(taskQueues = VacancyQueueProcessorConstants.QUEUE)
public class VacancyQueueProcessorWorkflowImpl implements VacancyQueueProcessorWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyQueueProcessorWorkflowImpl.class);

  // Флаг для остановки процесса
  private boolean shouldStop = false;

  private final VacancyQueueProcessorActivities activities = Workflow.newActivityStub(
      VacancyQueueProcessorActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setMaximumAttempts(3)
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumInterval(Duration.ofSeconds(10))
                  .setBackoffCoefficient(2.0)
                  .build()
          )
          .setStartToCloseTimeout(Duration.ofMinutes(15))
          .build()
  );

  @Override
  public void stop() {
    log.info("Received stop signal for queue processor workflow");
    this.shouldStop = true;
  }

  @Override
  public void processQueue(GenericTaskQueueType queueType) {
    if (queueType == null) {
      throw new IllegalArgumentException("queueType cannot be null");
    }

    log.info("Starting vacancy queue processor workflow for type: {}", queueType);

    int processedCount = 0;
    int successCount = 0;
    int failedCount = 0;

    try {
      while (true) {
        // Проверяем флаг остановки
        if (shouldStop) {
          log.warn("Queue processor stopped by user signal for type {}. Processed: {}, Success: {}, Failed: {}",
              queueType, processedCount, successCount, failedCount);
          return;
        }

        // 1. Получаем следующую задачу из очереди для указанного типа
        Optional<GenericTaskQueueDto> nextTask = activities.getNextTask(queueType);

        if (nextTask.isEmpty()) {
          log.info("No more NEW tasks in {} queue. Processed: {}, Success: {}, Failed: {}",
              queueType, processedCount, successCount, failedCount);
          break;
        }

        GenericTaskQueueDto task = nextTask.get();
        log.info("Processing {} task {} for vacancy: {}", queueType, task.getId(), task.getEntityId());

        try {
          // 2. Помечаем задачу как обрабатываемую
          activities.updateTaskStatus(task.getId(), GenericTaskQueueStatus.PROCESSING, null);

          // 3. Запускаем соответствующий workflow в зависимости от типа
          executeAnalysisWorkflow(queueType, task.getEntityId());

          // 4. Помечаем задачу как успешно завершенную
          log.info("{} task {} completed successfully", queueType, task.getId());
          activities.updateTaskStatus(task.getId(), GenericTaskQueueStatus.COMPLETED, null);
          successCount++;

        } catch (Exception e) {
          log.error("Error processing {} task {}: {}", queueType, task.getId(), e.getMessage());

          String errorMessage = "Workflow error: " + e.getMessage();
          if (errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 1000) + "...";
          }

          try {
            activities.updateTaskStatus(task.getId(), GenericTaskQueueStatus.FAILED, errorMessage);
          } catch (Exception saveError) {
            log.error("Failed to save error status for task {}: {}", task.getId(), saveError.getMessage());
          }
          failedCount++;
        }

        processedCount++;

        // Проверяем флаг остановки перед следующей итерацией
        if (shouldStop) {
          log.warn("Queue processor stopped by user signal for type {}. Processed: {}, Success: {}, Failed: {}",
              queueType, processedCount, successCount, failedCount);
          return;
        }
      }

      log.info("{} queue processing completed. Total: {}, Success: {}, Failed: {}",
          queueType, processedCount, successCount, failedCount);

    } catch (Exception e) {
      log.error("{} queue processor workflow failed: {}", queueType, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Запускает соответствующий analysis workflow в зависимости от типа очереди
   */
  private void executeAnalysisWorkflow(GenericTaskQueueType queueType, String vacancyId) {
    switch (queueType) {
      case LLM_FIRST:
        log.info("Executing FIRST analysis workflow for vacancy: {}", vacancyId);
        activities.executeVacancyFirstAnalysisWorkflow(vacancyId);
        break;

      case LLM_FULL:
        log.info("Executing FULL analysis workflow for vacancy: {}", vacancyId);
        activities.executeVacancyFullAnalysisWorkflow(vacancyId);
        break;

      case VACANCY_UPDATE:
        log.info("Executing VACANCY UPDATE workflow for vacancy: {}", vacancyId);
        activities.executeVacancyUpdateWorkflow(vacancyId);
        break;

      default:
        throw new IllegalArgumentException("Unsupported queue type: " + queueType);
    }
  }
}