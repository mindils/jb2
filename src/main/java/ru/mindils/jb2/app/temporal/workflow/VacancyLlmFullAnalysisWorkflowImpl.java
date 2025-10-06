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
import java.util.List;
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
public class VacancyLlmFullAnalysisWorkflowImpl implements VacancyLlmFullAnalysisWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyLlmFullAnalysisWorkflowImpl.class);

  /**
   * Все типы анализа после JAVA_PRIMARY в логическом порядке
   */
  private static final List<VacancyLlmAnalysisType> ANALYSIS_TYPES_AFTER_JAVA = List.of(
      STOP_FACTORS,     // Сначала проверяем стоп-факторы
      TECHNICAL,        // Затем техническая проверка
      WORK_CONDITIONS,  // Условия работы
      COMPENSATION,     // Компенсация
      BENEFITS,         // Льготы
      EQUIPMENT,        // Оборудование
      INDUSTRY          // Отрасль
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

    log.info("Starting FULL ANALYSIS workflow for vacancy: {} (refresh: {})", vacancyId, refresh);

    int successCount = 0;
    int totalSteps = ANALYSIS_TYPES_AFTER_JAVA.size() + 1; // +1 для JAVA_PRIMARY

    try {
      // === ПРОВЕРКА JAVA_PRIMARY ===
      log.info("Step 1/{}: Checking JAVA_PRIMARY for vacancy: {}", totalSteps, vacancyId);

      LlmAnalysisResponse javaResponse = analyzeStep(vacancyId, JAVA_PRIMARY, refresh);

      if (!activities.checkJavaTrue(javaResponse)) {
        log.warn("Java check failed for vacancy: {} - skipping all remaining analysis", vacancyId);

        String reason = "Stop because it's not Java";
        activities.setStatusSkip(vacancyId, Set.copyOf(ANALYSIS_TYPES_AFTER_JAVA), reason);

        log.info("Workflow completed early for vacancy {} - not a Java position", vacancyId);
        return;
      }

      log.info("Java check passed for vacancy: {} - proceeding with full analysis", vacancyId);
      successCount++;

      // === АНАЛИЗ СТОП-ФАКТОРОВ ===
      VacancyLlmAnalysisType stopFactorsType = ANALYSIS_TYPES_AFTER_JAVA.getFirst(); // STOP_FACTORS
      log.info("Step 2/{}: Running {} analysis for vacancy: {}", totalSteps, stopFactorsType, vacancyId);

      LlmAnalysisResponse stopFactorsResponse = analyzeStep(vacancyId, stopFactorsType, refresh);
      successCount++;

      if (activities.checkStopFactorsFound(stopFactorsResponse)) {
        log.warn("Stop factors found for vacancy: {} - skipping remaining analysis", vacancyId);

        String reason = "Critical stop factors detected during analysis";
        // Пропускаем все типы кроме STOP_FACTORS (индекс 0)
        List<VacancyLlmAnalysisType> remainingTypes = ANALYSIS_TYPES_AFTER_JAVA.subList(1, ANALYSIS_TYPES_AFTER_JAVA.size());
        activities.setStatusSkip(vacancyId, Set.copyOf(remainingTypes), reason);

        logFinalResult(vacancyId, successCount, totalSteps, "EARLY_TERMINATION_STOP_FACTORS");
        return;
      }

      log.info("No stop factors found for vacancy: {} - continuing with remaining analysis", vacancyId);

      // === ОСТАЛЬНЫЕ ТИПЫ АНАЛИЗА ===
      List<VacancyLlmAnalysisType> remainingTypes = ANALYSIS_TYPES_AFTER_JAVA.subList(1, ANALYSIS_TYPES_AFTER_JAVA.size());

      for (int i = 0; i < remainingTypes.size(); i++) {
        VacancyLlmAnalysisType analysisType = remainingTypes.get(i);
        int currentStep = i + 3; // +2 для JAVA_PRIMARY и STOP_FACTORS

        log.info("Step {}/{}: Processing {} analysis for vacancy: {}",
            currentStep, totalSteps, analysisType, vacancyId);

        try {
          analyzeStep(vacancyId, analysisType, refresh);
          successCount++;
          log.info("Step {}/{}: {} analysis completed successfully", currentStep, totalSteps, analysisType);

        } catch (Exception e) {
          log.error("Step {}/{}: {} analysis FAILED for vacancy {}: {}",
              currentStep, totalSteps, analysisType, vacancyId, e.getMessage());

          activities.saveAnalysisStatus(vacancyId, analysisType, VacancyLlmAnalysisStatus.ERROR);
        }
      }

      logFinalResult(vacancyId, successCount, totalSteps, "COMPLETED");
      log.info("FULL ANALYSIS WORKFLOW COMPLETED for vacancy: {}", vacancyId);

    } catch (Exception e) {
      log.error("CRITICAL FAILURE in full analysis workflow for vacancy {}: {}", vacancyId, e.getMessage(), e);
      logFinalResult(vacancyId, successCount, totalSteps, "CRITICAL_FAILURE");
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
    LlmAnalysisResponse response = activities.analyze(vacancyId, analysisType);
    activities.saveAnalysisResult(vacancyId, analysisType, response);

    if (response.hasParseError()) {
      log.warn("JSON parse error in {} analysis for vacancy {}: {}",
          analysisType, vacancyId, response.jsonParseError());
    }

    return response;
  }

  /**
   * Логирует финальный результат выполнения
   */
  private void logFinalResult(String vacancyId, int successCount, int totalSteps, String status) {
    log.info("=== WORKFLOW RESULT for vacancy: {} ===", vacancyId);
    log.info("Status: {}", status);
    log.info("Successful steps: {}/{}", successCount, totalSteps);
    log.info("=== END RESULT ===");
  }
}