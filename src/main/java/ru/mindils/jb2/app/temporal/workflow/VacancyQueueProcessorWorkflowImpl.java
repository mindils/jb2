package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.dto.GenericTaskQueueDto;
import ru.mindils.jb2.app.entity.GenericTaskQueue;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.temporal.VacancyQueueProcessorConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyQueueProcessorActivities;

import java.time.Duration;
import java.util.Optional;

@WorkflowImpl(taskQueues = VacancyQueueProcessorConstants.QUEUE)
public class VacancyQueueProcessorWorkflowImpl implements VacancyQueueProcessorWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyQueueProcessorWorkflowImpl.class);

  private final VacancyQueueProcessorActivities activities = Workflow.newActivityStub(
      VacancyQueueProcessorActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setMaximumAttempts(3)
                  .build()
          )
          .setStartToCloseTimeout(Duration.ofMinutes(10)) // Увеличиваем таймаут для запуска child workflow
          .build()
  );

  @Override
  public void processQueue() {
    log.info("Starting vacancy queue processor workflow");

    int processedCount = 0;
    int successCount = 0;
    int failedCount = 0;

    try {
      while (true) {
        // 1. Получаем следующую задачу из очереди со статусом NEW
        Optional<GenericTaskQueueDto> nextTask = activities.getNextLlmFirstTask();

        if (nextTask.isEmpty()) {
          log.info("No more NEW tasks in queue. Processed: {}, Success: {}, Failed: {}",
              processedCount, successCount, failedCount);
          break;
        }

        GenericTaskQueueDto task = nextTask.get();
        log.info("Processing task {} for vacancy: {}", task.getId(), task.getEntityId());

        try {
          // 2. Помечаем задачу как обрабатываемую
          activities.updateTaskStatus(task.getId(), GenericTaskQueueStatus.PROCESSING, null);

          // 3. Запускаем VacancyLlmFirstAnalysisWorkflow для анализа вакансии
          activities.executeVacancyAnalysisWorkflow(task.getEntityId());

          // 4. Помечаем задачу как успешно завершенную
          log.info("Task {} completed successfully", task.getId());
          activities.updateTaskStatus(task.getId(), GenericTaskQueueStatus.COMPLETED, null);
          successCount++;

        } catch (Exception e) {
          log.error("Error processing task {}: {}", task.getId(), e.getMessage());

          // Помечаем задачу как завершенную с ошибкой
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

        // Небольшая пауза между задачами
        Workflow.sleep(Duration.ofSeconds(2));
      }

      log.info("Queue processing completed. Total: {}, Success: {}, Failed: {}",
          processedCount, successCount, failedCount);

    } catch (Exception e) {
      log.error("Queue processor workflow failed: {}", e.getMessage(), e);
      throw e;
    }
  }
}