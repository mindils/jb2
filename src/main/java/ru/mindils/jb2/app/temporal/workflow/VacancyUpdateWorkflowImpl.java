package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.temporal.VacancyUpdateConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyUpdateActivities;

import java.time.Duration;

@WorkflowImpl(taskQueues = VacancyUpdateConstants.QUEUE)
public class VacancyUpdateWorkflowImpl implements VacancyUpdateWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyUpdateWorkflowImpl.class);

  private final VacancyUpdateActivities activities = Workflow.newActivityStub(
      VacancyUpdateActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
          .setStartToCloseTimeout(Duration.ofMinutes(2))
          .build()
  );

  @Override
  public void run() {
    log.info("Starting vacancy UPDATE workflow");

    try {
      while (true) {
        Long nextId = activities.getNextVacancyId();
        if (nextId == null) {
          log.info("No more vacancies to update. Exiting loop.");
          break;
        }
        log.info("Updating by queue id: {}", nextId);
        activities.update(nextId);

        // Небольшая пауза между обработкой вакансий
        Workflow.sleep(Duration.ofSeconds(1));
      }
      log.info("Vacancy UPDATE completed successfully.");
    } catch (Exception e) {
      log.error("Vacancy UPDATE failed: {}", e.getMessage(), e);
      throw e;
    }
  }
}
