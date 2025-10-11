package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.LoadContext;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.client.WorkflowClient;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysis;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisStatus;
import ru.mindils.jb2.app.repository.GenericTaskQueueRepository;
import ru.mindils.jb2.app.service.*;
import ru.mindils.jb2.app.temporal.MasterVacancyProcessingConstants;
import ru.mindils.jb2.app.temporal.VacancyQueueProcessorConstants;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ActivityImpl(taskQueues = MasterVacancyProcessingConstants.QUEUE)
public class MasterVacancyProcessingActivitiesImpl implements MasterVacancyProcessingActivities {

  private static final Logger log = LoggerFactory.getLogger(MasterVacancyProcessingActivitiesImpl.class);

  private final SystemAuthenticator authenticator;
  private final GenericTaskQueueRepository genericTaskQueueRepository;
  private final GenericTaskQueueService genericTaskQueueService;
  private final VacancyQueueProcessorWorkflowService queueProcessorWorkflowService;
  private final VacancyWorkflowService vacancyWorkflowService;
  private final VacancyScorerService vacancyScorerService;
  private final TemporalStatusService temporalStatusService;
  private final DataManager dataManager;
  private final FetchPlans fetchPlans;

  public MasterVacancyProcessingActivitiesImpl(
      SystemAuthenticator authenticator,
      GenericTaskQueueRepository genericTaskQueueRepository,
      GenericTaskQueueService genericTaskQueueService,
      VacancyQueueProcessorWorkflowService queueProcessorWorkflowService,
      VacancyWorkflowService vacancyWorkflowService,
      VacancyScorerService vacancyScorerService,
      TemporalStatusService temporalStatusService,
      DataManager dataManager,
      FetchPlans fetchPlans) {
    this.authenticator = authenticator;
    this.genericTaskQueueRepository = genericTaskQueueRepository;
    this.genericTaskQueueService = genericTaskQueueService;
    this.queueProcessorWorkflowService = queueProcessorWorkflowService;
    this.vacancyWorkflowService = vacancyWorkflowService;
    this.vacancyScorerService = vacancyScorerService;
    this.temporalStatusService = temporalStatusService;
    this.dataManager = dataManager;
    this.fetchPlans = fetchPlans;
  }

  @Override
  public int enqueueJavaVacanciesForUpdate() {
    return authenticator.withSystem(() -> {
      log.info("Enqueueing Java vacancies for update");

      int count = genericTaskQueueRepository.enqueueJavaVacanciesForUpdate();

      log.info("Enqueued {} Java vacancies for update", count);
      return count;
    });
  }

  @Override
  public void processUpdateQueue() {
    log.info("Starting update queue processing");

    String workflowId = VacancyQueueProcessorConstants.WORKFLOW_ID + "_" +
        GenericTaskQueueType.VACANCY_UPDATE.getId();

    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      log.warn("Update queue processor already running, waiting for completion");
      waitForWorkflowCompletion(workflowId, 120);
      return;
    }

    queueProcessorWorkflowService.startVacancyUpdateQueueProcessing();
    waitForWorkflowCompletion(workflowId, 120);

    log.info("Update queue processing completed");
  }

  @Override
  public void syncRecentVacancies(int days) {
    log.info("Syncing vacancies for last {} days", days);

    String workflowId = VacancySyncConstants.WORKFLOW_ID;

    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      log.warn("Sync workflow already running, waiting for completion");
      waitForWorkflowCompletion(workflowId, 60);
      return;
    }

    vacancyWorkflowService.sync(List.of(Map.of("period", String.valueOf(days))));
    waitForWorkflowCompletion(workflowId, 60);

    log.info("Recent vacancies sync completed");
  }

  @Override
  public int enqueueForFullAnalysis() {
    return authenticator.withSystem(() -> {
      log.info("Enqueueing vacancies for full LLM analysis");

      int count = genericTaskQueueService.enqueueFullLlmAnalysis();

      log.info("Enqueued {} vacancies for full analysis", count);
      return count;
    });
  }

  @Override
  public void processFullAnalysisQueue() {
    log.info("Starting full analysis queue processing");

    String workflowId = VacancyQueueProcessorConstants.WORKFLOW_ID + "_" +
        GenericTaskQueueType.LLM_FULL.getId();

    if (temporalStatusService.isWorkflowRunning(workflowId)) {
      log.warn("Full analysis queue processor already running, waiting for completion");
      waitForWorkflowCompletion(workflowId, 180);
      return;
    }

    queueProcessorWorkflowService.startFullAnalysisQueueProcessing();
    waitForWorkflowCompletion(workflowId, 180);

    log.info("Full analysis queue processing completed");
  }

  @Override
  public String calculateVacancyScores() {
    return authenticator.withSystem(() -> {
      log.info("Calculating vacancy scores for Java vacancies");

      // Загружаем все ID Java вакансий которые не архивированы
      List<String> vacancyIds = dataManager.loadValues(
              "select e.id from jb2_VVacancySearch e " +
                  "where e.isJavaVacancy = true " +
                  "and (e.archived is null or e.archived = false)"
          )
          .properties("id")
          .list()
          .stream()
          .map(kv -> kv.<String>getValue("id"))
          .collect(Collectors.toList());

      log.info("Found {} Java vacancies for scoring", vacancyIds.size());

      if (vacancyIds.isEmpty()) {
        return "Total: 0, Successful: 0, Failed: 0, Skipped: 0";
      }

      // Обрабатываем батчами
      int batchSize = 100;
      int total = vacancyIds.size();
      int successful = 0;
      int failed = 0;
      int skipped = 0;
      List<String> errors = new ArrayList<>();

      for (int i = 0; i < vacancyIds.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, vacancyIds.size());
        List<String> batch = vacancyIds.subList(i, endIndex);

        log.info("Processing scoring batch {}-{} of {}", i + 1, endIndex, total);

        for (String vacancyId : batch) {
          try {
            // Загружаем анализы для вакансии
            List<VacancyLlmAnalysis> analyses = dataManager
                .load(VacancyLlmAnalysis.class)
                .query("select e from jb2_VacancyLlmAnalysis e " +
                    "where e.vacancy.id = :vacancyId " +
                    "and e.status = :status " +
                    "and e.analyzeData is not null")
                .parameter("vacancyId", vacancyId)
                .parameter("status", VacancyLlmAnalysisStatus.DONE.getId())
                .fetchPlan(fetchPlans.builder(VacancyLlmAnalysis.class)
                    .addAll("analyzeType", "analyzeData", "status")
                    .build())
                .list();

            if (analyses.isEmpty()) {
              skipped++;
              log.debug("No completed analysis for vacancy {}, skipping", vacancyId);
              continue;
            }

            // Рассчитываем и сохраняем оценку
            vacancyScorerService.calculateAndSave(vacancyId);
            successful++;

          } catch (Exception e) {
            failed++;
            String errorMsg = String.format("Error processing vacancy %s: %s",
                vacancyId, e.getMessage());
            log.error(errorMsg, e);
            errors.add(errorMsg);
          }
        }
      }

      String resultStr = String.format(
          "Total: %d, Successful: %d, Failed: %d, Skipped: %d",
          total,
          successful,
          failed,
          skipped
      );

      log.info("Vacancy scores calculation completed: {}", resultStr);

      if (!errors.isEmpty()) {
        log.warn("Errors during scoring (showing first 10): {}",
            errors.stream().limit(10).collect(Collectors.toList()));
      }

      return resultStr;
    });
  }

  @Override
  public boolean isWorkflowRunning(String workflowId) {
    return temporalStatusService.isWorkflowRunning(workflowId);
  }

  @Override
  public void waitForWorkflowCompletion(String workflowId, int timeoutMinutes) {
    log.info("Waiting for workflow {} completion (timeout: {} minutes)", workflowId, timeoutMinutes);

    long startTime = System.currentTimeMillis();
    long timeoutMs = timeoutMinutes * 60 * 1000L;

    while (temporalStatusService.isWorkflowRunning(workflowId)) {
      long elapsed = System.currentTimeMillis() - startTime;

      if (elapsed > timeoutMs) {
        throw new RuntimeException(
            "Timeout waiting for workflow " + workflowId +
                " completion after " + timeoutMinutes + " minutes"
        );
      }

      try {
        // Проверяем каждые 10 секунд
        Thread.sleep(10000);

        if (elapsed % 60000 < 10000) { // Логируем каждую минуту
          log.info("Workflow {} still running, elapsed: {} minutes",
              workflowId, elapsed / 60000);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for workflow completion", e);
      }
    }

    log.info("Workflow {} completed", workflowId);
  }
}