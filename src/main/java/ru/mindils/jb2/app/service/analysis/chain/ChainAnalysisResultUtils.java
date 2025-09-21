// ru/mindils/jb2/app/service/analysis/chain/ChainAnalysisResultUtils.java
package ru.mindils.jb2.app.service.analysis.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.service.analysis.VacancyScorer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Утилиты для работы с результатами цепочки анализа
 */
@Component
public class ChainAnalysisResultUtils {

  private final ObjectMapper objectMapper;

  public ChainAnalysisResultUtils(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Создать JSON-отчет по результатам анализа
   */
  public JsonNode createReportJson(ChainAnalysisResult result) {
    ObjectNode report = objectMapper.createObjectNode();

    report.put("vacancyId", result.vacancyId());
    report.put("chainId", result.chainConfig().chainId().getId());
    report.put("success", result.success());
    report.put("timestamp", LocalDateTime.now().toString());

    if (result.errorMessage() != null) {
      report.put("errorMessage", result.errorMessage());
    }

    if (result.stoppedAt() != null) {
      report.put("stoppedAt", result.stoppedAt());
      report.put("stopReason", result.stopReason());
    }

    // Результаты каждого шага
    ObjectNode stepResults = objectMapper.createObjectNode();
    result.stepResults().forEach((stepId, stepResult) -> {
      ObjectNode stepNode = objectMapper.createObjectNode();
      stepNode.put("success", stepResult.shouldContinue());
      if (!stepResult.shouldContinue()) {
        stepNode.put("stopReason", stepResult.stopReason());
      }
      if (stepResult.stepData() != null) {
        stepNode.set("data", stepResult.stepData());
      }
      stepResults.set(stepId, stepNode);
    });
    report.set("stepResults", stepResults);

    // Итоговый скор
    if (result.finalScore() != null) {
      ObjectNode scoreNode = objectMapper.createObjectNode();
      scoreNode.put("totalScore", result.finalScore().totalScore());
      scoreNode.put("rating", result.finalScore().rating().toString());

      // Разбивка скора
      VacancyScorer.ScoreBreakdown breakdown = result.finalScore().breakdown();
      ObjectNode breakdownNode = objectMapper.createObjectNode();
      breakdownNode.put("javaScore", breakdown.javaScore);
      breakdownNode.put("jmixScore", breakdown.jmixScore);
      breakdownNode.put("aiScore", breakdown.aiScore);
      breakdownNode.put("workModeScore", breakdown.workModeScore);
      breakdownNode.put("socialSignificanceScore", breakdown.socialSignificanceScore);
      breakdownNode.put("roleTypeScore", breakdown.roleTypeScore);
      breakdownNode.put("positionLevelScore", breakdown.positionLevelScore);
      breakdownNode.put("stackScore", breakdown.stackScore);
      breakdownNode.put("salaryScore", breakdown.salaryScore);
      breakdownNode.put("companyScore", breakdown.companyScore);
      breakdownNode.put("completenessBonus", breakdown.completenessBonus);

      // Итоговые суммы по категориям
      breakdownNode.put("primaryTotal", breakdown.primaryTotal);
      breakdownNode.put("socialTotal", breakdown.socialTotal);
      breakdownNode.put("technicalTotal", breakdown.technicalTotal);

      scoreNode.set("breakdown", breakdownNode);
      report.set("finalScore", scoreNode);
    }

    return report;
  }


  /**
   * Создать краткое текстовое описание результата
   */
  public String createSummaryText(ChainAnalysisResult result) {
    StringBuilder summary = new StringBuilder();

    summary.append("Анализ вакансии ").append(result.vacancyId());
    summary.append(" (цепочка: ").append(result.chainConfig().chainId()).append(")");

    if (!result.success()) {
      summary.append(" - ОШИБКА: ").append(result.errorMessage());
      return summary.toString();
    }

    if (result.stoppedAt() != null) {
      summary.append(" - остановлен на шаге '").append(result.stoppedAt()).append("'");
      summary.append(" (").append(result.stopReason()).append(")");
    } else {
      summary.append(" - завершен успешно");
    }

    if (result.finalScore() != null) {
      VacancyScorer.VacancyScore score = result.finalScore();
      summary.append(", итоговый скор: ").append(score.totalScore());
      summary.append(" (").append(score.rating()).append(")");
      summary.append(" [").append(String.format("%.1f%%", score.getScorePercentage())).append("]");
    }

    return summary.toString();
  }


  /**
   * Извлечь ключевые метрики из результата
   */
  public Map<String, Object> extractKeyMetrics(ChainAnalysisResult result) {
    return Map.of(
        "vacancyId", result.vacancyId(),
        "chainId", result.chainConfig().chainId(),
        "success", result.success(),
        "stepsExecuted", result.stepResults().size(),
        "stoppedEarly", result.stoppedAt() != null,
        "finalScore", result.finalScore() != null ? result.finalScore().totalScore() : null,
        "rating", result.finalScore() != null ? result.finalScore().rating().toString() : null,
        "scorePercentage", result.finalScore() != null ? result.finalScore().getScorePercentage() : null
    );
  }

  /**
   * Получить список выполненных шагов
   */
  public String getExecutedStepsText(ChainAnalysisResult result) {
    return result.stepResults().entrySet().stream()
        .map(entry -> {
          String stepId = entry.getKey();
          ChainStepResult stepResult = entry.getValue();
          return stepId + (stepResult.shouldContinue() ? " ✓" : " ✗");
        })
        .collect(Collectors.joining(", "));
  }

  /**
   * Создать детальный отчет по скору
   */
  public String createDetailedScoreReport(ChainAnalysisResult result) {
    if (result.finalScore() == null) {
      return "Скор не рассчитан";
    }

    VacancyScorer.VacancyScore score = result.finalScore();
    VacancyScorer.ScoreBreakdown breakdown = score.breakdown();

    StringBuilder report = new StringBuilder();
    report.append("=== ДЕТАЛЬНЫЙ СКОР ===\n");
    report.append("Общий скор: ").append(score.totalScore())
        .append(" из ").append(VacancyScorer.VacancyScore.getMaxPossibleScore())
        .append(" (").append(String.format("%.1f%%", score.getScorePercentage())).append(")\n");
    report.append("Рейтинг: ").append(score.getDescription()).append("\n\n");

    // Первичный анализ
    if (breakdown.primaryTotal > 0) {
      report.append("📊 Первичный анализ: ").append(breakdown.primaryTotal).append(" баллов\n");
      if (breakdown.javaScore > 0) report.append("  • Java: ").append(breakdown.javaScore).append("\n");
      if (breakdown.jmixScore > 0) report.append("  • Jmix: ").append(breakdown.jmixScore).append("\n");
      if (breakdown.aiScore > 0) report.append("  • AI: ").append(breakdown.aiScore).append("\n");
      report.append("\n");
    }

    // Социальный анализ
    if (breakdown.socialTotal > 0) {
      report.append("🏠 Социальный анализ: ").append(breakdown.socialTotal).append(" баллов\n");
      if (breakdown.workModeScore > 0) report.append("  • Формат работы: ").append(breakdown.workModeScore).append("\n");
      if (breakdown.socialSignificanceScore > 0) report.append("  • Социальная значимость: ").append(breakdown.socialSignificanceScore).append("\n");
      report.append("\n");
    }

    // Технический анализ
    if (breakdown.technicalTotal > 0) {
      report.append("⚙️ Технический анализ: ").append(breakdown.technicalTotal).append(" баллов\n");
      if (breakdown.roleTypeScore > 0) report.append("  • Тип роли: ").append(breakdown.roleTypeScore).append("\n");
      if (breakdown.positionLevelScore > 0) report.append("  • Уровень позиции: ").append(breakdown.positionLevelScore).append("\n");
      if (breakdown.stackScore > 0) report.append("  • Технологический стек: ").append(breakdown.stackScore).append("\n");
      report.append("\n");
    }

    // Дополнительные критерии
    if (breakdown.salaryScore > 0 || breakdown.companyScore > 0 || breakdown.completenessBonus > 0) {
      report.append("➕ Дополнительные баллы:\n");
      if (breakdown.salaryScore > 0) report.append("  • Зарплата: ").append(breakdown.salaryScore).append("\n");
      if (breakdown.companyScore > 0) report.append("  • Компания: ").append(breakdown.companyScore).append("\n");
      if (breakdown.completenessBonus > 0) report.append("  • Полнота анализа: ").append(breakdown.completenessBonus).append("\n");
    }

    // Основная причина скора
    report.append("\n💡 Основная причина: ").append(breakdown.getMainScoreReason());

    return report.toString();
  }

  /**
   * Проверить качество результата анализа
   */
  public AnalysisQuality assessAnalysisQuality(ChainAnalysisResult result) {
    if (!result.success()) {
      return new AnalysisQuality(
          QualityLevel.POOR,
          "Анализ завершился с ошибкой",
          List.of("Исправить ошибки в процессе анализа")
      );
    }

    int stepsCompleted = result.stepResults().size();
    boolean hasScore = result.finalScore() != null;
    boolean stoppedEarly = result.stoppedAt() != null;

    if (stepsCompleted >= 3 && hasScore && !stoppedEarly) {
      return new AnalysisQuality(
          QualityLevel.EXCELLENT,
          "Полный качественный анализ",
          List.of()
      );
    } else if (stepsCompleted >= 2 && hasScore) {
      return new AnalysisQuality(
          QualityLevel.GOOD,
          "Достаточно полный анализ",
          stoppedEarly ? List.of("Рассмотреть продолжение анализа") : List.of()
      );
    } else if (stepsCompleted >= 1) {
      return new AnalysisQuality(
          QualityLevel.MODERATE,
          "Базовый анализ выполнен",
          List.of("Добавить дополнительные шаги анализа", "Рассчитать итоговый скор")
      );
    } else {
      return new AnalysisQuality(
          QualityLevel.POOR,
          "Недостаточно данных для анализа",
          List.of("Выполнить хотя бы первичный анализ")
      );
    }
  }

  /**
   * Качество анализа
   */
  public static record AnalysisQuality(
      QualityLevel level,
      String description,
      java.util.List<String> recommendations
  ) {}

  /**
   * Уровень качества анализа
   */
  public enum QualityLevel {
    EXCELLENT, GOOD, MODERATE, POOR
  }
}