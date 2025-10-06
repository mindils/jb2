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
import java.util.Set;

import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.BENEFITS;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.COMPENSATION;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.EQUIPMENT;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.INDUSTRY;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.JAVA_PRIMARY;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.STOP_FACTORS;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.TECHNICAL;
import static ru.mindils.jb2.app.entity.VacancyLlmAnalysisType.WORK_CONDITIONS;

@WorkflowImpl(taskQueues = VacancyLlmAnalysisConstants.QUEUE)
public class VacancyLlmFirstAnalysisWorkflowImpl implements VacancyLlmFirstAnalysisWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyLlmFirstAnalysisWorkflowImpl.class);

  /**
   * Все типы анализа, которые должны быть пропущены если Java = false
   */
  private static final Set<VacancyLlmAnalysisType> TYPES_TO_SKIP_IF_NOT_JAVA = Set.of(
      STOP_FACTORS,
      BENEFITS,
      COMPENSATION,
      EQUIPMENT,
      INDUSTRY,
      TECHNICAL,
      WORK_CONDITIONS
  );

  private final VacancyLllAnalysisActivities activities =
      Workflow.newActivityStub(VacancyLllAnalysisActivities.class,
          ActivityOptions.newBuilder()
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setMaximumAttempts(3)
                      .setInitialInterval(Duration.ofSeconds(1))
                      .setMaximumInterval(Duration.ofSeconds(10))
                      .setBackoffCoefficient(2.0)
                      .build()
              )
              .setStartToCloseTimeout(Duration.ofMinutes(3))
              .build());

  @Override
  public void run(String vacancyId, Boolean refresh) {
    if (vacancyId == null || vacancyId.trim().isEmpty()) {
      throw new IllegalArgumentException("vacancyId cannot be null or empty");
    }

    log.info("Starting FIRST ANALYSIS workflow for vacancy: {} (refresh: {})", vacancyId, refresh);

    try {
      // === АНАЛИЗ JAVA_PRIMARY ===
      log.info("Running JAVA_PRIMARY analysis for vacancy: {}", vacancyId);

      LlmAnalysisResponse javaResponse = analyzeStep(vacancyId, JAVA_PRIMARY, refresh);

      // Проверяем результат Java анализа
      boolean isJavaPosition = activities.checkJavaTrue(javaResponse);

      if (!isJavaPosition) {
        log.warn("Java check failed for vacancy: {} - marking other analysis types as SKIPPED", vacancyId);

        String reason = "Stop because it's not Java";
        activities.setStatusSkip(vacancyId, TYPES_TO_SKIP_IF_NOT_JAVA, reason);

        logFinalResult(vacancyId, "NOT_JAVA_POSITION");
        return;
      }

      log.info("Java check passed for vacancy: {} - position is suitable for Java developers", vacancyId);
      logFinalResult(vacancyId, "COMPLETED_JAVA_POSITION");

    } catch (Exception e) {
      log.error("CRITICAL FAILURE in first analysis workflow for vacancy {}: {}", vacancyId, e.getMessage(), e);

      // Сохраняем статус ошибки для JAVA_PRIMARY
      activities.saveAnalysisStatus(vacancyId, JAVA_PRIMARY, VacancyLlmAnalysisStatus.ERROR);

      logFinalResult(vacancyId, "CRITICAL_FAILURE");
      throw e;
    }
  }

  /**
   * Выполняет один шаг анализа с учетом параметра refresh
   */
  private LlmAnalysisResponse analyzeStep(String vacancyId,
                                          VacancyLlmAnalysisType analysisType,
                                          Boolean refresh) {

    // Если refresh != true, проверяем существующий результат
    if (!Boolean.TRUE.equals(refresh)) {
      if (activities.hasExistingAnalysis(vacancyId, analysisType)) {
        log.info("Found existing analysis for vacancy {} type {} - skipping (refresh=false)",
            vacancyId, analysisType);
        return activities.getExistingAnalysis(vacancyId, analysisType);
      }
    }

    // Выполняем новый анализ
    log.info("Performing fresh {} analysis for vacancy {} ({})",
        analysisType, vacancyId,
        Boolean.TRUE.equals(refresh) ? "forced refresh" : "no existing data");

    LlmAnalysisResponse response = activities.analyze(vacancyId, analysisType);
    activities.saveAnalysisResult(vacancyId, analysisType, response);

    // Логируем результат
    log.info("{} analysis completed - LLM Call: {}, Valid JSON: {}",
        analysisType, response.llmCallId(), response.hasValidJson());

    if (response.hasParseError()) {
      log.warn("JSON parse error in {} analysis for vacancy {}: {}",
          analysisType, vacancyId, response.jsonParseError());
    }

    return response;
  }

  /**
   * Логирует финальный результат выполнения
   */
  private void logFinalResult(String vacancyId, String status) {
    log.info("=== FIRST ANALYSIS RESULT for vacancy: {} ===", vacancyId);
    log.info("Status: {}", status);

    switch (status) {
      case "COMPLETED_JAVA_POSITION":
        log.info("Result: Position is suitable for Java developers - ready for full analysis");
        break;
      case "NOT_JAVA_POSITION":
        log.info("Result: Position is not for Java developers - other analysis types skipped");
        break;
      case "CRITICAL_FAILURE":
        log.info("Result: Analysis failed due to critical error");
        break;
    }

    log.info("=== END RESULT ===");
  }
}