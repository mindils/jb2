package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.temporal.MasterVacancyProcessingConstants;
import ru.mindils.jb2.app.temporal.acrivity.MasterVacancyProcessingActivities;

import java.time.Duration;

@WorkflowImpl(taskQueues = MasterVacancyProcessingConstants.QUEUE)
public class MasterVacancyProcessingWorkflowImpl implements MasterVacancyProcessingWorkflow {

  private static final Logger log = Workflow.getLogger(MasterVacancyProcessingWorkflowImpl.class);

  private final MasterVacancyProcessingActivities activities = Workflow.newActivityStub(
      MasterVacancyProcessingActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setMaximumAttempts(3)
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumInterval(Duration.ofSeconds(10))
                  .setBackoffCoefficient(2.0)
                  .build()
          )
          .setStartToCloseTimeout(Duration.ofHours(4)) // Долгий timeout для всего процесса
          .setHeartbeatTimeout(Duration.ofMinutes(5))
          .build()
  );

  @Override
  public void processAllVacancies(int syncDays) {
    log.info("=== STARTING MASTER VACANCY PROCESSING WORKFLOW ===");
    log.info("Sync days parameter: {}", syncDays);

    try {
      // ШАГ 1: Добавление Java вакансий в очередь на обновление
      log.info("STEP 1/6: Enqueueing Java vacancies for update");
      int enqueuedForUpdate = activities.enqueueJavaVacanciesForUpdate();
      log.info("STEP 1 COMPLETED: Enqueued {} vacancies for update", enqueuedForUpdate);

      // ШАГ 2: Обработка очереди обновления
      log.info("STEP 2/6: Processing update queue");
      activities.processUpdateQueue();
      log.info("STEP 2 COMPLETED: Update queue processed");

      // ШАГ 3: Синхронизация недавних вакансий
      log.info("STEP 3/6: Syncing recent vacancies ({} days)", syncDays);
      activities.syncRecentVacancies(syncDays);
      log.info("STEP 3 COMPLETED: Recent vacancies synced");

      // ШАГ 4: Добавление вакансий на полный анализ
      log.info("STEP 4/6: Enqueueing vacancies for full analysis");
      int enqueuedForAnalysis = activities.enqueueForFullAnalysis();
      log.info("STEP 4 COMPLETED: Enqueued {} vacancies for full analysis", enqueuedForAnalysis);

      // ШАГ 5: Обработка полного анализа
      log.info("STEP 5/6: Processing full analysis queue");
      activities.processFullAnalysisQueue();
      log.info("STEP 5 COMPLETED: Full analysis queue processed");

      // ШАГ 6: Расчет оценок вакансий
      log.info("STEP 6/6: Calculating vacancy scores");
      String scoringResult = activities.calculateVacancyScores();
      log.info("STEP 6 COMPLETED: Vacancy scores calculated - {}", scoringResult);

      log.info("=== MASTER VACANCY PROCESSING WORKFLOW COMPLETED SUCCESSFULLY ===");
      log.info("Summary:");
      log.info("  - Vacancies enqueued for update: {}", enqueuedForUpdate);
      log.info("  - Vacancies enqueued for analysis: {}", enqueuedForAnalysis);
      log.info("  - Scoring result: {}", scoringResult);

    } catch (Exception e) {
      log.error("=== MASTER VACANCY PROCESSING WORKFLOW FAILED ===");
      log.error("Error: {}", e.getMessage(), e);
      throw e;
    }
  }
}