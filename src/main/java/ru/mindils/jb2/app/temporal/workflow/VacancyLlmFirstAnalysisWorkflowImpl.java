package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.temporal.VacancyLlmAnalysisConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyLllAnalysisActivities;

import java.time.Duration;
import java.util.Optional;

@WorkflowImpl(taskQueues = VacancyLlmAnalysisConstants.QUEUE)
public class VacancyLlmFirstAnalysisWorkflowImpl implements VacancyLlmFirstAnalysisWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyLlmFirstAnalysisWorkflowImpl.class);

  private final VacancyLllAnalysisActivities activities =
      Workflow.newActivityStub(VacancyLllAnalysisActivities.class,
          ActivityOptions.newBuilder()
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setMaximumAttempts(3)
                      .build()
              )
              .setStartToCloseTimeout(Duration.ofMinutes(2))
              .build());

  @Override
  public void run(String vacancyId, Boolean refresh) {
    log.info("Starting first analyze workflow for vacancyId: {}", vacancyId);

    try {
      String llmResponse = activities.analyze(vacancyId, VacancyLlmAnalysisType.JAVA_PRIMARY);

      Optional<String> error = activities.saveAnalysisResult(vacancyId, VacancyLlmAnalysisType.JAVA_PRIMARY, llmResponse);

      if (error.isPresent()) {
        // todo: сохранить куда нибудь ошибку парсинга при сохранении
      }
    } catch (Exception e) {
      log.error("Vacancy analyze first failed for vacancyId {}: {}", vacancyId, e.getMessage(), e);
      throw e;
    }
  }
}