package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.temporal.VacancyAnalysisConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyAnalysisActivities;

import java.time.Duration;

@WorkflowImpl(taskQueues = VacancyAnalysisConstants.QUEUE)
public class VacancyAnalysisWorkflowImpl implements VacancyAnalysisWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyAnalysisWorkflowImpl.class);

  private final VacancyAnalysisActivities activities =
      Workflow.newActivityStub(VacancyAnalysisActivities.class,
          ActivityOptions.newBuilder()
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setMaximumAttempts(3)
                      .build()
              )
              .setStartToCloseTimeout(Duration.ofMinutes(2))
              .build());

  @Override
  public void run(AnalysisType type) {
    log.info("Starting analyze workflow for type: {}", type);

    try {
      while (true) {
        Long nextVacancyId = activities.getNextVacancyId(type);
        if (nextVacancyId == null) {
          log.info("No more vacancies to process for type {}. Exiting loop.", type);
          break;
        }
        log.info("Analyzing vacancy with queue ID: {}", nextVacancyId);
        activities.analyze(nextVacancyId, type);
      }
      log.info("Vacancy analyze completed successfully for type {}.", type);

    } catch (Exception e) {
      log.error("Vacancy analyze failed for type {}: {}", type, e.getMessage(), e);
      throw e;
    }
  }
}