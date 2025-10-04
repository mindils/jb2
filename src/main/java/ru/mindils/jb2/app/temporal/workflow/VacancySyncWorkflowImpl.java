package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.dto.VacancyShortDto;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancySyncActivities;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@WorkflowImpl(taskQueues = VacancySyncConstants.VACANCY_QUEUE)
public class VacancySyncWorkflowImpl implements VacancySyncWorkflow {
  private static final Logger log = Workflow.getLogger(VacancySyncWorkflowImpl.class);

  // Флаг для остановки процесса
  private boolean shouldStop = false;

  private final VacancySyncActivities activities = Workflow.newActivityStub(VacancySyncActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setMaximumAttempts(3)
                  .build()
          )
          .setStartToCloseTimeout(Duration.ofMinutes(2))
          .build());

  @Override
  public void stop() {
    log.info("Received stop signal for vacancy synchronization workflow");
    this.shouldStop = true;
  }

  @Override
  public void run(List<Map<String, String>> requestParams) {
    log.info("Starting vacancy synchronization workflow with custom params: {}", requestParams);

    int currentPage = 0;
    int totalPages = 0;
    int totalProcessed = 0;
    int totalVacancies = 0;

    try {
      do {
        // Проверяем флаг остановки
        if (shouldStop) {
          log.warn("Vacancy synchronization stopped by user signal at page {}/{}. Processed {} vacancies",
              currentPage, totalPages, totalProcessed);
          return;
        }

        log.info("Processing page: {}", currentPage);

        VacancySearchResponseDto response = activities.searchVacancies(currentPage, requestParams);

        if (currentPage == 0) {
          totalPages = response.getPages();
          totalVacancies = response.getFound();
          log.info("Total pages to process: {}, total vacancies found: {}", totalPages, totalVacancies);

          if (totalPages == 0 || response.getItems().isEmpty()) {
            log.warn("No vacancies found to process");
            break;
          }
        }

        for (VacancyShortDto vacancy : response.getItems()) {
          // Проверяем флаг остановки перед обработкой каждой вакансии
          if (shouldStop) {
            log.warn("Vacancy synchronization stopped by user signal at vacancy {}/{}. Total processed: {}",
                totalProcessed, totalVacancies, totalProcessed);
            return;
          }

          try {
            log.debug("Processing vacancy: {} - {}", vacancy.getId(), vacancy.getName());
            activities.saveVacancy(vacancy.getId());
            totalProcessed++;
            Workflow.sleep(Duration.ofMillis(100));
          } catch (Exception e) {
            log.error("Failed to process vacancy {}: {}", vacancy.getId(), e.getMessage());
          }
        }

        currentPage++;
        double progress = (double) currentPage / totalPages * 100;
        log.info("Processed page {}/{} ({:.1f}%), vacancies processed: {}",
            currentPage, totalPages, progress, totalProcessed);

        if (currentPage < totalPages) {
          Workflow.sleep(Duration.ofSeconds(1));
        }
      } while (currentPage < totalPages);

      log.info("Vacancy synchronization completed successfully. " +
          "Total pages processed: {}, total vacancies processed: {}", currentPage, totalProcessed);
    } catch (Exception e) {
      log.error("Vacancy synchronization failed: {}", e.getMessage(), e);
      throw e;
    }
  }
}