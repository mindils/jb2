package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.dto.VacancyShortDto;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueueType;
import ru.mindils.jb2.app.service.VacancyAnalysisService;
import ru.mindils.jb2.app.temporal.VacancyAnalysisConstants;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyAnalysisActivities;
import ru.mindils.jb2.app.temporal.acrivity.VacancySyncActivities;

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
  public void run() {
    log.info("Starting analyze workflow");

    try {
      while (true) {
        Long nextVacancy = activities.getNextVacancyId(VacancyAnalysisQueueType.FIRST);
        if (nextVacancy == null) {
          break;
        }
        activities.analyze(nextVacancy);
      }
      log.info("Vacancy analyze completed successfully.");

    } catch (Exception e) {
      log.error("Vacancy analyze failed: {}", e.getMessage(), e);
      throw e;
    }
  }
}