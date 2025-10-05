package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.VacancyUpdateConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancySyncActivities;

import java.time.Duration;

@WorkflowImpl(taskQueues = VacancySyncConstants.VACANCY_QUEUE)
public class VacancyUpdateWorkflowImpl implements VacancyUpdateWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyUpdateWorkflowImpl.class);

  private final VacancySyncActivities activities = Workflow.newActivityStub(
      VacancySyncActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setMaximumAttempts(3)
                  .setInitialInterval(Duration.ofSeconds(1))
                  .setMaximumInterval(Duration.ofSeconds(10))
                  .setBackoffCoefficient(2.0)
                  .build()
          )
          .setStartToCloseTimeout(Duration.ofMinutes(5))
          .build()
  );

  @Override
  public void updateVacancy(String vacancyId) {
    log.info("Starting vacancy update workflow for vacancy: {}", vacancyId);

    try {
      activities.saveVacancy(vacancyId);
      log.info("Successfully updated vacancy: {}", vacancyId);
    } catch (Exception e) {
      log.error("Failed to update vacancy {}: {}", vacancyId, e.getMessage(), e);
      throw e;
    }
  }
}