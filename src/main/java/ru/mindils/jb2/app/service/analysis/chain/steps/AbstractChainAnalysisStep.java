package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisStep;
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

/**
 * Базовый абстрактный класс для шагов анализа с поддержкой кэширования результатов
 */
public abstract class AbstractChainAnalysisStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(AbstractChainAnalysisStep.class);

  protected final ObjectMapper objectMapper;
  protected final AnalysisResultManager analysisResultManager;

  protected AbstractChainAnalysisStep(ObjectMapper objectMapper,
                                      AnalysisResultManager analysisResultManager) {
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing step '{}' for vacancy: {}", getStepId(), vacancy.getId());

    try {
      // Проверяем, есть ли уже кэшированный результат
      JsonNode cachedResult = getCachedStepResult(currentAnalysis);

      if (cachedResult != null && !shouldForceReanalyze()) {
        log.info("Using cached result for step '{}' and vacancy: {}", getStepId(), vacancy.getId());

        // Обновляем метаданные, но не перезаписываем сам результат
        analysisResultManager.updateStepResult(currentAnalysis, getStepId(), cachedResult);

        // Проверяем условия остановки на основе кэшированных данных
        if (analysisResultManager.shouldStopPipeline(currentAnalysis, getStepId())) {
          String stopReason = determineStopReason(cachedResult);
          return ChainStepResult.stop(stopReason, cachedResult, "cached_result");
        }

        return ChainStepResult.success(cachedResult, "cached_result");
      }

      // Выполняем новый анализ через LLM
      log.info("Running fresh analysis for step '{}' and vacancy: {} ({})",
          getStepId(), vacancy.getId(),
          cachedResult != null ? "force reanalyze" : "no cached result");

      return executeNewAnalysis(vacancy, currentAnalysis);

    } catch (Exception e) {
      log.error("Error in step '{}' for vacancy {}: {}", getStepId(), vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed step: " + getStepId(), e);
    }
  }

  /**
   * Получить кэшированный результат для данного шага
   */
  protected JsonNode getCachedStepResult(VacancyAnalysis analysis) {
    if (analysis.getStepResults() == null) {
      return null;
    }

    JsonNode stepResult = analysis.getStepResult(getStepId());

    // Проверяем, что результат не пустой и содержит ожидаемые поля
    if (stepResult != null && !stepResult.isNull() && stepResult.size() > 0) {
      log.debug("Found cached result for step '{}': {}", getStepId(), stepResult);
      return stepResult;
    }

    return null;
  }

  /**
   * Определить, нужно ли принудительно перезапускать анализ
   * По умолчанию false - можно переопределить в подклассах
   */
  protected boolean shouldForceReanalyze() {
    return false;
  }

  /**
   * Определить причину остановки на основе кэшированных данных
   * Может быть переопределено в подклассах для специфической логики
   */
  protected String determineStopReason(JsonNode cachedResult) {
    return "Условия остановки сработали на основе кэшированных данных";
  }

  /**
   * Выполнить новый анализ через LLM
   * Должно быть реализовано в подклассах
   */
  protected abstract ChainStepResult executeNewAnalysis(Vacancy vacancy, VacancyAnalysis currentAnalysis);

  /**
   * Сохранить результат анализа и обновить метаданные
   */
  protected ChainStepResult saveAnalysisResult(VacancyAnalysis currentAnalysis,
                                               JsonNode analysisResult,
                                               String llmResponse) {
    // Сохраняем результат в новую структуру
    analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

    // Проверяем условие остановки
    if (analysisResultManager.shouldStopPipeline(currentAnalysis, getStepId())) {
      String stopReason = determineStopReason(analysisResult);
      return ChainStepResult.stop(stopReason, analysisResult, llmResponse);
    }

    return ChainStepResult.success(analysisResult, llmResponse);
  }

  /**
   * Обрезать текст до указанной длины
   */
  protected String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}