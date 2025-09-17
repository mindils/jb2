package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.dto.VacancyShortDto;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancySyncActivities;

import java.time.Duration;

@WorkflowImpl(taskQueues = VacancySyncConstants.VACANCY_QUEUE)
public class VacancySyncWorkflowImpl implements VacancySyncWorkflow {

  private static final Logger log = Workflow.getLogger(VacancySyncWorkflowImpl.class);

  // Настройки активностей
  private final VacancySyncActivities activities =
      Workflow.newActivityStub(VacancySyncActivities.class,
          ActivityOptions.newBuilder()
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setMaximumAttempts(3)
                      .build()
              )
              .setStartToCloseTimeout(Duration.ofMinutes(2))
              .build());

  @Override
  public void run() {
    log.info("Starting vacancy synchronization workflow");

    // Создаем stub для вызова активностей
    int currentPage = 0;
    int totalPages = 0;
    int totalProcessed = 0;
    int totalVacancies = 0;

    try {
      do {
        log.info("Processing page: {}", currentPage);

        // Получаем вакансии с текущей страницы
        VacancySearchResponseDto response = activities.searchVacancies(currentPage);

        // При первом запросе узнаем общее количество страниц
        if (currentPage == 0) {
          totalPages = response.getPages();
          totalVacancies = response.getFound();
          log.info("Total pages to process: {}, total vacancies found: {}", totalPages, totalVacancies);

          // Проверяем, есть ли вакансии для обработки
          if (totalPages == 0 || response.getItems().isEmpty()) {
            log.warn("No vacancies found to process");
            break;
          }
        }

        // Обрабатываем каждую вакансию на текущей странице
        for (VacancyShortDto vacancy : response.getItems()) {
          try {
            log.debug("Processing vacancy: {} - {}", vacancy.getId(), vacancy.getName());
            activities.saveVacancy(vacancy.getId());
            totalProcessed++;

            // Добавляем небольшую задержку между запросами для избежания rate limiting
            Workflow.sleep(Duration.ofMillis(100));

          } catch (Exception e) {
            log.error("Failed to process vacancy {}: {}", vacancy.getId(), e.getMessage());
            // Продолжаем обработку других вакансий, даже если одна не удалась
          }
        }

        currentPage++;

        // Прогресс
        double progress = (double) currentPage / totalPages * 100;
        log.info("Processed page {}/{} ({:.1f}%), vacancies processed: {}",
            currentPage, totalPages, progress, totalProcessed);

        // Задержка между страницами для снижения нагрузки на API
        if (currentPage < totalPages) {
          Workflow.sleep(Duration.ofSeconds(1));
        }

      } while (currentPage < totalPages);

      log.info("Vacancy synchronization completed successfully. " +
              "Total pages processed: {}, total vacancies processed: {}",
          currentPage, totalProcessed);

      activities.saveVacancyState();

    } catch (Exception e) {
      log.error("Vacancy synchronization failed: {}", e.getMessage(), e);
      throw e;
    }
  }
}