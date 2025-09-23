package ru.mindils.jb2.app.service.analysis.chain;

import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.VacancyScorer;
import ru.mindils.jb2.app.service.analysis.chain.steps.AbstractChainAnalysisStep;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Упрощенный сервис для выполнения цепочки анализа вакансий с поддержкой кэширования
 */
@Service
public class VacancyChainAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(VacancyChainAnalysisService.class);

  private final DataManager dataManager;
  private final AnalysisResultManager analysisResultManager;
  private final Map<String, ChainAnalysisStep> stepMap;

  public VacancyChainAnalysisService(
      DataManager dataManager,
      AnalysisResultManager analysisResultManager,
      List<ChainAnalysisStep> steps
  ) {
    this.dataManager = dataManager;
    this.analysisResultManager = analysisResultManager;
    this.stepMap = steps.stream()
        .collect(Collectors.toMap(ChainAnalysisStep::getStepId, Function.identity()));
  }

  /**
   * Выполняет цепочку анализа для вакансии
   */
  @Transactional
  public ChainAnalysisResult executeChain(String vacancyId, AnalysisChainConfig config) {
    log.info("Starting chain analysis for vacancy: {} with config: {} (forceReanalyze: {})",
        vacancyId, config.chainId(), config.forceReanalyze());

    try {
      // Загружаем вакансию и анализ
      Vacancy vacancy = dataManager.load(Vacancy.class).id(vacancyId).one();
      VacancyAnalysis analysis = dataManager.load(VacancyAnalysis.class)
          .id(vacancyId)
          .optional()
          .orElseGet(() -> createNewAnalysis(vacancy));

      ChainAnalysisResult.Builder resultBuilder = ChainAnalysisResult.builder()
          .vacancyId(vacancyId)
          .chainConfig(config)
          .success(true);

      // Выполняем шаги по очереди
      for (String stepId : config.stepIds()) {
        ChainAnalysisStep step = stepMap.get(stepId);
        if (step == null) {
          log.error("Step not found: {}", stepId);
          return resultBuilder
              .success(false)
              .errorMessage("Step not found: " + stepId)
              .build();
        }

        // Устанавливаем флаг принудительного перезапуска для шагов, которые его поддерживают
        configureStepForExecution(step, config);

        log.info("Executing step: {} for vacancy: {} (forceReanalyze: {})",
            stepId, vacancyId, config.forceReanalyze());

        ChainStepResult stepResult = step.execute(vacancy, analysis);
        resultBuilder.addStepResult(stepId, stepResult);

        // Логируем информацию о том, использовался ли кэш
        logStepExecution(stepId, stepResult, config.forceReanalyze());

        // Сохраняем промежуточный результат
        dataManager.save(analysis);

        // Проверяем условие остановки
        if (!stepResult.shouldContinue()) {
          log.info("Chain stopped at step: {} for vacancy: {}, reason: {}",
              stepId, vacancyId, stepResult.stopReason());

          // Если нужно, все равно считаем скор даже при остановке
          VacancyScorer.VacancyScore finalScore = calculateFinalScoreIfNeeded(config, analysis);
          if (finalScore != null) {
            dataManager.save(analysis); // Сохраняем с обновленным скором
          }

          return resultBuilder
              .stoppedAt(stepId)
              .stopReason(stepResult.stopReason())
              .finalScore(finalScore)
              .build();
        }
      }

      // Если нужно, вычисляем итоговый скор после всех шагов
      VacancyScorer.VacancyScore finalScore = calculateFinalScoreIfNeeded(config, analysis);
      if (finalScore != null) {
        dataManager.save(analysis); // Сохраняем с итоговым скором
        log.info("Calculated final score for vacancy {}: {} ({})",
            vacancyId, finalScore.totalScore(), finalScore.rating());
      }

      log.info("Successfully completed chain analysis for vacancy: {}", vacancyId);

      return resultBuilder
          .finalScore(finalScore)
          .build();

    } catch (Exception e) {
      log.error("Error in chain analysis for vacancy {}: {}", vacancyId, e.getMessage(), e);
      return ChainAnalysisResult.builder()
          .vacancyId(vacancyId)
          .chainConfig(config)
          .success(false)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  /**
   * Настраивает шаг для выполнения с учетом конфигурации
   */
  private void configureStepForExecution(ChainAnalysisStep step, AnalysisChainConfig config) {
    // Если шаг поддерживает настройку принудительного перезапуска
    if (step instanceof AbstractChainAnalysisStep abstractStep) {
      // Здесь мы можем добавить дополнительную логику настройки
      log.debug("Configuring step {} with forceReanalyze: {}", step.getStepId(), config.forceReanalyze());
    }

    // Для конкретных шагов с методом setForceReanalyze
    try {
      var setForceReanalyzeMethod = step.getClass().getMethod("setForceReanalyze", boolean.class);
      setForceReanalyzeMethod.invoke(step, config.forceReanalyze());
      log.debug("Set forceReanalyze={} for step: {}", config.forceReanalyze(), step.getStepId());
    } catch (Exception e) {
      // Метод не найден или не доступен - это нормально для шагов, которые его не поддерживают
      log.debug("Step {} does not support setForceReanalyze method", step.getStepId());
    }
  }

  /**
   * Логирует информацию о выполнении шага
   */
  private void logStepExecution(String stepId, ChainStepResult stepResult, boolean forceReanalyze) {
    if (stepResult.llmResponse().equals("cached_result")) {
      log.info("✓ Step '{}' used cached result (forceReanalyze was disabled)", stepId);
    } else {
      log.info("⚡ Step '{}' executed fresh LLM analysis{}", stepId,
          forceReanalyze ? " (forced reanalysis)" : " (no cached result)");
    }
  }

  /**
   * Вычисляет итоговый скор, если это необходимо
   */
  private VacancyScorer.VacancyScore calculateFinalScoreIfNeeded(AnalysisChainConfig config, VacancyAnalysis analysis) {
    if (!config.calculateScore()) {
      return null;
    }

    analysisResultManager.recalculateAndSaveScore(analysis);
    return new VacancyScorer.VacancyScore(
        analysis.getFinalScore() != null ? analysis.getFinalScore() : 0,
        analysis.getRatingEnum() != null ? analysis.getRatingEnum() : ru.mindils.jb2.app.entity.VacancyRating.VERY_POOR
    );
  }

  private VacancyAnalysis createNewAnalysis(Vacancy vacancy) {
    VacancyAnalysis analysis = dataManager.create(VacancyAnalysis.class);
    analysis.setId(vacancy.getId());
    analysis.setVacancy(vacancy);
    return analysis;
  }

  /**
   * Получить статистику кэширования для цепочки
   */
  public ChainCacheStats getCacheStats(String vacancyId) {
    VacancyAnalysis analysis = dataManager.load(VacancyAnalysis.class)
        .id(vacancyId)
        .optional()
        .orElse(null);

    if (analysis == null || analysis.getStepResults() == null) {
      return new ChainCacheStats(0, 0, List.of());
    }

    var stepResults = analysis.getStepResults();
    int totalSteps = stepResults.size();

    // Здесь можно добавить логику подсчета, какие шаги использовали кэш
    // На данный момент просто возвращаем базовую статистику

    return new ChainCacheStats(
        totalSteps,
        totalSteps, // предполагаем, что все шаги кэшированы
        List.of(stepResults.fieldNames().next()) // список названий шагов
    );
  }

  /**
   * Статистика кэширования для цепочки
   */
  public static record ChainCacheStats(
      int totalSteps,
      int cachedSteps,
      java.util.List<String> cachedStepIds
  ) {
    public double getCacheHitRate() {
      return totalSteps > 0 ? (double) cachedSteps / totalSteps : 0.0;
    }

    public String getSummary() {
      return String.format("Кэш: %d/%d шагов (%.1f%%)",
          cachedSteps, totalSteps, getCacheHitRate() * 100);
    }
  }
}