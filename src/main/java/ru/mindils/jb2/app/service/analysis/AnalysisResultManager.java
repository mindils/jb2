package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

import java.time.LocalDateTime;

/**
 * Упрощенный сервис для управления результатами анализа
 */
@Service
public class AnalysisResultManager {

  private final ObjectMapper objectMapper;
  private final VacancyScorer vacancyScorer;

  public AnalysisResultManager(ObjectMapper objectMapper, VacancyScorer vacancyScorer) {
    this.objectMapper = objectMapper;
    this.vacancyScorer = vacancyScorer;
  }

  /**
   * Обновить результат шага анализа
   */
  public void updateStepResult(VacancyAnalysis analysis, String stepId, JsonNode stepData) {
    ObjectNode stepResults = getOrCreateStepResults(analysis);
    stepResults.set(stepId, stepData);
    analysis.setStepResults(stepResults);

    updateAnalysisMetadata(analysis, stepId);
  }

  /**
   * Пересчитать и сохранить финальный скор
   */
  public void recalculateAndSaveScore(VacancyAnalysis analysis) {
    VacancyScorer.VacancyScore score = vacancyScorer.calculateScore(analysis);

    // Сохраняем скор и рейтинг в анализе
    analysis.setFinalScore(score.totalScore());
    analysis.setRating(score.rating().getId());

    updateAnalysisMetadata(analysis, "score_calculated");
  }

  /**
   * Проверить, можно ли остановить pipeline на основе результатов
   */
  public boolean shouldStopPipeline(VacancyAnalysis analysis, String afterStep) {
    return switch (afterStep) {
      case "primary" -> shouldStopAfterPrimary(analysis);
      case "social" -> shouldStopAfterSocial(analysis);
      case "technical" -> shouldStopAfterTechnical(analysis);
      default -> false;
    };
  }

  /**
   * Получить или создать объект результатов шагов
   */
  private ObjectNode getOrCreateStepResults(VacancyAnalysis analysis) {
    if (analysis.getStepResults() == null || !analysis.getStepResults().isObject()) {
      return objectMapper.createObjectNode();
    }
    return (ObjectNode) analysis.getStepResults();
  }

  /**
   * Обновить метаданные анализа
   */
  private void updateAnalysisMetadata(VacancyAnalysis analysis, String lastOperation) {
    ObjectNode metadata = analysis.getAnalysisMetadata() != null && analysis.getAnalysisMetadata().isObject()
        ? (ObjectNode) analysis.getAnalysisMetadata()
        : objectMapper.createObjectNode();

    metadata.put("lastUpdated", LocalDateTime.now().toString());
    metadata.put("lastOperation", lastOperation);

    if (!metadata.has("createdAt")) {
      metadata.put("createdAt", LocalDateTime.now().toString());
    }

    // Статистика выполненных шагов
    if (analysis.getStepResults() != null && analysis.getStepResults().isObject()) {
      metadata.put("completedSteps", analysis.getStepResults().size());
    }

    analysis.setAnalysisMetadata(metadata);
  }

  /**
   * Условия остановки после первичного анализа
   */
  private boolean shouldStopAfterPrimary(VacancyAnalysis analysis) {
    JsonNode primaryResult = analysis.getStepResult("primary");
    if (primaryResult == null) return false;

    // Останавливаем если не Java
    return !primaryResult.path("java").asBoolean(false);
  }

  /**
   * Условия остановки после социального анализа
   */
  private boolean shouldStopAfterSocial(VacancyAnalysis analysis) {
    JsonNode socialResult = analysis.getStepResult("social");
    if (socialResult == null) return false;

    // Останавливаем если только офис и не социально значимый
    String workMode = socialResult.path("work_mode").asText("");
    boolean isSocial = socialResult.path("socially_significant").asBoolean(true);

    return "office".equals(workMode) && !isSocial;
  }

  /**
   * Условия остановки после технического анализа
   */
  private boolean shouldStopAfterTechnical(VacancyAnalysis analysis) {
    JsonNode technicalResult = analysis.getStepResult("technical");
    if (technicalResult == null) return false;

    // Останавливаем если роль не разработка
    String roleType = technicalResult.path("role_type").asText("");
    return "other".equals(roleType);
  }
}