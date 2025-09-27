package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.dto.LlmAnalysisResponse;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisStatus;
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
      // Получаем DTO с результатом анализа и метаинформацией
      LlmAnalysisResponse llmResponse = activities.analyze(vacancyId, VacancyLlmAnalysisType.JAVA_PRIMARY);

      log.info("Analysis completed for vacancy {}, LLM call ID: {}, JSON valid: {}",
          vacancyId, llmResponse.llmCallId(), llmResponse.hasValidJson());

      // Если java false установим всем шагам skip чтобы не обрабатывать их потом
      activities.setStatusSkipIfJavaFalse(vacancyId, llmResponse);

      // Сохраняем результат анализа
      activities.saveAnalysisResult(vacancyId, VacancyLlmAnalysisType.JAVA_PRIMARY, llmResponse);

      if (llmResponse.hasParseError()) {
        log.warn("LLM response JSON parse error for vacancy {}: {}",
            vacancyId, llmResponse.jsonParseError());
        // TODO: можно добавить логику для повторного анализа или уведомлений
      }

      if (llmResponse.hasValidJson()) {
        log.info("Successfully completed analysis with valid JSON for vacancy {}", vacancyId);
      } else {
        log.warn("Analysis completed but JSON is invalid for vacancy {}", vacancyId);
      }

    } catch (Exception e) {
      log.error("Vacancy analyze first failed for vacancyId {}: {}", vacancyId, e.getMessage(), e);
      activities.saveAnalysisStatus(vacancyId, VacancyLlmAnalysisType.JAVA_PRIMARY, VacancyLlmAnalysisStatus.ERROR);

      throw e;
    }
  }
}