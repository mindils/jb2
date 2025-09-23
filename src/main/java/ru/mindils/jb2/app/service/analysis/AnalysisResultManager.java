package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.entity.VacancyRating;

import java.time.LocalDateTime;

/**
 * Сервис для управления результатами анализа
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
   * Удалить результат шага
   */
  public void removeStepResult(VacancyAnalysis analysis, String stepId) {
    if (analysis.getStepResults() == null || !analysis.getStepResults().isObject()) {
      return;
    }

    ObjectNode stepResults = (ObjectNode) analysis.getStepResults();
    stepResults.remove(stepId);
    analysis.setStepResults(stepResults);

    updateAnalysisMetadata(analysis, "removed_" + stepId);
  }

  /**
   * Пересчитать оценку на основе текущих результатов
   */
  public void recalculateScore(VacancyAnalysis analysis) {
    VacancyScorer.VacancyScore score = vacancyScorer.calculateScore(analysis);
    analysis.setFinalScore(score.totalScore());

    // ИСПРАВЛЕНО: правильная конвертация enum'ов
    VacancyRating rating = convertToVacancyRating(score.rating());
    analysis.setRating(rating.getId()); // Используем строковое представление

    updateAnalysisMetadata(analysis, "score_calculation");
  }

  /**
   * Проверить, можно ли остановить pipeline на основе результатов
   */
  public boolean shouldStopPipeline(VacancyAnalysis analysis, String afterStep) {
    return switch (afterStep) {
      case "primary" -> shouldStopAfterPrimary(analysis);
      case "social" -> shouldStopAfterSocial(analysis);
      case "technical" -> shouldStopAfterTechnical(analysis);
      case "salary" -> shouldStopAfterSalary(analysis);
      case "company_research" -> shouldStopAfterCompanyResearch(analysis);
      default -> false;
    };
  }

  /**
   * Получить текстовое описание результатов анализа
   */
  public String getAnalysisSummary(VacancyAnalysis analysis) {
    if (analysis.getStepResults() == null) {
      return "Анализ не выполнялся";
    }

    StringBuilder summary = new StringBuilder();

    // Основные результаты
    JsonNode primary = analysis.getStepResult("primary");
    if (primary != null) {
      summary.append("Java: ").append(primary.path("java").asBoolean() ? "Да" : "Нет");
      if (primary.path("jmix").asBoolean()) summary.append(", Jmix");
      if (primary.path("ai").asBoolean()) summary.append(", AI");
    }

    JsonNode social = analysis.getStepResult("social");
    if (social != null) {
      String workMode = social.path("work_mode").asText("");
      if (!workMode.isEmpty()) {
        summary.append(", Формат: ").append(formatWorkMode(workMode));
      }
    }

    if (analysis.getFinalScore() != null) {
      summary.append(", Скор: ").append(analysis.getFinalScore());
    }

    return summary.toString();
  }

  /**
   * Проверить готовность для расчета скора
   */
  public boolean isReadyForScoring(VacancyAnalysis analysis) {
    // Минимум нужен первичный анализ
    return analysis.hasStepResult("primary");
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

    // Добавляем статистику выполненных шагов
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

  /**
   * Условия остановки после анализа зарплаты
   */
  private boolean shouldStopAfterSalary(VacancyAnalysis analysis) {
    JsonNode salaryResult = analysis.getStepResult("salary");
    if (salaryResult == null) return false;

    // Можно остановить если зарплата слишком низкая
    int salaryFrom = salaryResult.path("salary_from").asInt(0);
    return salaryFrom > 0 && salaryFrom < 80000; // Меньше 80k рублей
  }

  /**
   * Условия остановки после исследования компании
   */
  private boolean shouldStopAfterCompanyResearch(VacancyAnalysis analysis) {
    JsonNode companyResult = analysis.getStepResult("company_research");
    if (companyResult == null) return false;

    // Можно остановить если плохая репутация компании
    return companyResult.path("bad_reputation").asBoolean(false);
  }

  /**
   * Конвертация из VacancyScorer.Rating в VacancyRating enum
   */
  private VacancyRating convertToVacancyRating(VacancyRating rating) {
    return switch (rating) {
      case EXCELLENT -> VacancyRating.EXCELLENT;
      case GOOD -> VacancyRating.GOOD;
      case MODERATE -> VacancyRating.MODERATE;
      case POOR -> VacancyRating.POOR;
      case VERY_POOR -> VacancyRating.VERY_POOR;
    };
  }

  /**
   * Форматирование режима работы для отображения
   */
  private String formatWorkMode(String workMode) {
    return switch (workMode) {
      case "remote" -> "Удаленно";
      case "flexible" -> "Гибкий";
      case "hybrid" -> "Гибрид";
      case "hybrid_2_3" -> "Гибрид 2/3";
      case "hybrid_3_2" -> "Гибрид 3/2";
      case "hybrid_4_1" -> "Гибрид 4/1";
      case "hybrid_flexible" -> "Гибкий гибрид";
      case "office" -> "Офис";
      default -> workMode;
    };
  }

  /**
   * Получить статистику по всем выполненным шагам
   */
  public String getStepsStatistics(VacancyAnalysis analysis) {
    if (analysis.getStepResults() == null) {
      return "Нет данных";
    }

    StringBuilder stats = new StringBuilder();
    stats.append("Выполнено шагов: ").append(analysis.getStepResults().size());

    if (analysis.hasStepResult("primary")) stats.append(" [Первичный]");
    if (analysis.hasStepResult("social")) stats.append(" [Социальный]");
    if (analysis.hasStepResult("technical")) stats.append(" [Технический]");
    if (analysis.hasStepResult("salary")) stats.append(" [Зарплата]");
    if (analysis.hasStepResult("company_research")) stats.append(" [Компания]");

    return stats.toString();
  }
}